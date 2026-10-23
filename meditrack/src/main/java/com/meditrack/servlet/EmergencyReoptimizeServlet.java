package com.meditrack.servlet;

import com.meditrack.model.CollectionAssignment;
import com.meditrack.model.Collector;
import com.meditrack.model.RouteUpdate;
import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.RouteOptimizer;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * EmergencyReoptimizeServlet
 * ---------------------------
 * THE MAIN UNIQUE FEATURE: Dynamic Route Re-Optimization.
 *
 * When a new HIGH-priority ("emergency") waste request comes in while a
 * collector already has an ACTIVE route, this logic:
 *   1. Finds all collectors with an ACTIVE assignment.
 *   2. For the nearest/most suitable active collector, uses cheapest-
 *      insertion (RouteOptimizer.insertEmergency) to splice the new stop
 *      into their current route at the position that adds the least extra
 *      travel distance, while emergency stops are still prioritized.
 *   3. Saves a routeUpdates document capturing the OLD route and the NEW
 *      route + distances, so the dashboard can render a side-by-side
 *      "old vs updated route" comparison.
 *   4. Updates the waste record to ASSIGNED + linked collector.
 *
 * Exposed both as:
 *   - a static helper (called directly from WasteSubmitServlet for instant
 *     reaction at the moment of submission), and
 *   - a GET endpoint /admin/reoptimize?wasteId=... for manual/admin trigger
 *     or testing.
 */
@WebServlet("/admin/reoptimize")
public class EmergencyReoptimizeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String wasteId = req.getParameter("wasteId");
        MongoDatabase db = MongoDBConnection.getDatabase();
        boolean changed = reoptimizeForEmergency(db, wasteId);
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard?reoptimized=" + changed);
    }

    /**
     * Core logic - finds the best ACTIVE collector route to splice the
     * emergency waste point into. Returns true if a route was updated.
     */
    public static boolean reoptimizeForEmergency(MongoDatabase db, String wasteId) {
        MongoCollection<Document> wasteCol = db.getCollection("wasteRecords");
        Document wasteDoc = wasteCol.find(new Document("_id", new ObjectId(wasteId))).first();
        if (wasteDoc == null) return false;

        MongoCollection<Document> hospitalsCol = db.getCollection("hospitals");
        Document hospitalDoc = hospitalsCol.find(new Document("_id", new ObjectId(wasteDoc.getString("hospitalId")))).first();
        if (hospitalDoc == null) return false;

        RouteOptimizer.Stop emergencyStop = new RouteOptimizer.Stop(
                wasteId,
                wasteDoc.getString("hospitalId"),
                hospitalDoc.getString("name"),
                ((Number) hospitalDoc.get("latitude")).doubleValue(),
                ((Number) hospitalDoc.get("longitude")).doubleValue(),
                ((Number) wasteDoc.get("priorityScore")).doubleValue(),
                true
        );

        MongoCollection<Document> assignmentsCol = db.getCollection("collectionAssignments");
        FindIterable<Document> activeAssignments = assignmentsCol.find(new Document("status", "ACTIVE"));

        // Pick the active assignment whose nearest current stop is geographically
        // closest to the emergency location - that collector can reach it fastest.
        Document bestAssignmentDoc = null;
        double bestDistance = Double.MAX_VALUE;

        for (Document a : activeAssignments) {
            @SuppressWarnings("unchecked")
            List<Document> route = (List<Document>) a.get("route");
            if (route == null || route.isEmpty()) continue;

            for (Document stop : route) {
                double d = RouteOptimizer.haversineDistanceKm(
                        ((Number) stop.get("latitude")).doubleValue(),
                        ((Number) stop.get("longitude")).doubleValue(),
                        emergencyStop.latitude, emergencyStop.longitude);
                if (d < bestDistance) {
                    bestDistance = d;
                    bestAssignmentDoc = a;
                }
            }
        }

        if (bestAssignmentDoc == null) {
            // No collector currently on an active route - nothing to re-optimize.
            // The waste record simply stays PENDING and will be picked up on the
            // next manual/auto route generation (RouteOptimizeServlet).
            return false;
        }

        CollectionAssignment assignment = CollectionAssignment.fromDocument(bestAssignmentDoc);
        String collectorId = assignment.getCollectorId();

        MongoCollection<Document> collectorsCol = db.getCollection("collectors");
        Collector collector = Collector.fromDocument(collectorsCol.find(new Document("_id", new ObjectId(collectorId))).first());
        double startLat = collector != null ? collector.getCurrentLat() : 0;
        double startLng = collector != null ? collector.getCurrentLng() : 0;

        List<RouteOptimizer.Stop> oldRoute = RouteOptimizeServlet.fromRouteDocs(assignment.getRoute());
        double oldDistance = RouteOptimizer.totalRouteDistanceKm(startLat, startLng, oldRoute);

        List<RouteOptimizer.Stop> newRoute = RouteOptimizer.insertEmergency(startLat, startLng, oldRoute, emergencyStop);
        double newDistance = RouteOptimizer.totalRouteDistanceKm(startLat, startLng, newRoute);

        // Persist updated assignment
        assignmentsCol.updateOne(new Document("_id", new ObjectId(assignment.getId())),
                new Document("$set", new Document("route", RouteOptimizeServlet.toRouteDocs(newRoute))
                        .append("totalDistanceKm", newDistance)
                        .append("updatedAt", Instant.now().toString())));

        // Record the before/after comparison in routeUpdates
        RouteUpdate update = new RouteUpdate();
        update.setCollectorId(collectorId);
        update.setTriggeringWasteId(wasteId);
        update.setReason("New HIGH-priority emergency waste reported at " + hospitalDoc.getString("name"));
        update.setOldRoute(RouteOptimizeServlet.toRouteDocs(oldRoute));
        update.setNewRoute(RouteOptimizeServlet.toRouteDocs(newRoute));
        update.setOldDistanceKm(oldDistance);
        update.setNewDistanceKm(newDistance);
        update.setCreatedAt(Instant.now().toString());

        db.getCollection("routeUpdates").insertOne(update.toDocument());

        // Mark the emergency waste record as ASSIGNED to this collector
        wasteCol.updateOne(new Document("_id", new ObjectId(wasteId)),
                new Document("$set", new Document("status", "ASSIGNED").append("assignedCollectorId", collectorId)));

        return true;
    }
}
