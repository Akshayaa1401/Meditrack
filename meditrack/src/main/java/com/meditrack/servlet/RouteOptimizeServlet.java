package com.meditrack.servlet;

import com.meditrack.model.CollectionAssignment;
import com.meditrack.model.Collector;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * RouteOptimizeServlet
 * ---------------------
 * Triggered by a collector (or admin, on their behalf) to generate/refresh
 * an optimized collection route, using priority-weighted nearest neighbor
 * (see RouteOptimizer.buildRoute). Includes both brand-new PENDING waste
 * and waste already assigned to this collector that hasn't been collected
 * yet — so regenerating never silently drops stops you're already working on.
 *
 * Any existing ACTIVE assignment for this collector is marked SUPERSEDED
 * and a new one created. Waste records included in the route are marked
 * ASSIGNED and linked to this collector.
 */
@WebServlet("/collector/optimizeRoute")
public class RouteOptimizeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String collectorId = (String) session.getAttribute("collectorId");

        if (collectorId == null) {
            resp.sendRedirect(req.getContextPath() + "/collector/dashboard?error=no_collector_profile");
            return;
        }

        MongoDatabase db = MongoDBConnection.getDatabase();
        buildAndSaveRoute(db, collectorId);

        resp.sendRedirect(req.getContextPath() + "/collector/dashboard?optimized=true");
    }

    /**
     * Core route-building logic, reusable from other servlets (e.g. could be
     * called by an admin tool too). Pulls in:
     *   1) all PENDING waste records (never assigned to anyone yet), AND
     *   2) waste already ASSIGNED to THIS collector that hasn't been
     *      collected yet (so clicking "Re-generate route" again never
     *      silently wipes out stops you're already working on).
     * Joins with hospital coordinates, runs the optimizer, and persists
     * the result.
     */
    public static CollectionAssignment buildAndSaveRoute(MongoDatabase db, String collectorId) {
        MongoCollection<Document> collectorsCol = db.getCollection("collectors");
        Collector collector = Collector.fromDocument(collectorsCol.find(new Document("_id", new ObjectId(collectorId))).first());
        double startLat = collector != null ? collector.getCurrentLat() : 0;
        double startLng = collector != null ? collector.getCurrentLng() : 0;

        MongoCollection<Document> wasteCol = db.getCollection("wasteRecords");
        MongoCollection<Document> hospitalsCol = db.getCollection("hospitals");

        Document filter = new Document("$or", List.of(
                new Document("status", "PENDING"),
                new Document("status", "ASSIGNED").append("assignedCollectorId", collectorId)
        ));
        FindIterable<Document> candidates = wasteCol.find(filter);

        List<RouteOptimizer.Stop> stops = new ArrayList<>();
        for (Document w : candidates) {
            String hospitalId = w.getString("hospitalId");
            Document h = hospitalsCol.find(new Document("_id", new ObjectId(hospitalId))).first();
            if (h == null) continue;

            RouteOptimizer.Stop stop = new RouteOptimizer.Stop(
                    w.getObjectId("_id").toHexString(),
                    hospitalId,
                    h.getString("name"),
                    ((Number) h.get("latitude")).doubleValue(),
                    ((Number) h.get("longitude")).doubleValue(),
                    ((Number) w.get("priorityScore")).doubleValue(),
                    w.getBoolean("emergency", false)
            );
            stops.add(stop);
        }

        List<RouteOptimizer.Stop> route = RouteOptimizer.buildRoute(startLat, startLng, stops);
        double totalDistance = RouteOptimizer.totalRouteDistanceKm(startLat, startLng, route);

        // Mark any prior ACTIVE assignment for this collector as COMPLETED (superseded)
        MongoCollection<Document> assignmentsCol = db.getCollection("collectionAssignments");
        assignmentsCol.updateMany(
                new Document("collectorId", collectorId).append("status", "ACTIVE"),
                new Document("$set", new Document("status", "SUPERSEDED")));

        CollectionAssignment assignment = new CollectionAssignment();
        assignment.setCollectorId(collectorId);
        assignment.setRoute(toRouteDocs(route));
        assignment.setTotalDistanceKm(totalDistance);
        assignment.setStatus("ACTIVE");
        assignment.setCreatedAt(Instant.now().toString());
        assignment.setUpdatedAt(Instant.now().toString());

        Document doc = assignment.toDocument();
        assignmentsCol.insertOne(doc);
        assignment.setId(doc.getObjectId("_id").toHexString());

        // Mark included waste records ASSIGNED
        for (RouteOptimizer.Stop s : route) {
            wasteCol.updateOne(new Document("_id", new ObjectId(s.wasteId)),
                    new Document("$set", new Document("status", "ASSIGNED").append("assignedCollectorId", collectorId)));
        }

        // Mark collector ON_ROUTE
        collectorsCol.updateOne(new Document("_id", new ObjectId(collectorId)),
                new Document("$set", new Document("status", "ON_ROUTE")));

        return assignment;
    }

    public static List<Document> toRouteDocs(List<RouteOptimizer.Stop> route) {
        List<Document> docs = new ArrayList<>();
        for (RouteOptimizer.Stop s : route) {
            docs.add(new Document("wasteId", s.wasteId)
                    .append("hospitalId", s.hospitalId)
                    .append("hospitalName", s.hospitalName)
                    .append("latitude", s.latitude)
                    .append("longitude", s.longitude)
                    .append("priorityScore", s.priorityScore)
                    .append("emergency", s.emergency));
        }
        return docs;
    }

    public static List<RouteOptimizer.Stop> fromRouteDocs(List<Document> docs) {
        List<RouteOptimizer.Stop> stops = new ArrayList<>();
        if (docs == null) return stops;
        for (Document d : docs) {
            stops.add(new RouteOptimizer.Stop(
                    d.getString("wasteId"),
                    d.getString("hospitalId"),
                    d.getString("hospitalName"),
                    ((Number) d.get("latitude")).doubleValue(),
                    ((Number) d.get("longitude")).doubleValue(),
                    ((Number) d.get("priorityScore")).doubleValue(),
                    d.getBoolean("emergency", false)
            ));
        }
        return stops;
    }
}
