package com.meditrack.servlet;

import com.google.gson.Gson;
import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * CollectorLocationServlet
 * --------------------------
 * Two endpoints:
 *
 *   POST /api/collectorLocation
 *     Called by the collector's browser (via JS navigator.geolocation) to
 *     update their current position on active waste records.
 *     Body params: wasteId, latitude, longitude
 *     Updates the wasteRecord with collectorLatitude/collectorLongitude/lastLocationUpdate.
 *
 *   GET /api/collectorLocation?wasteId=xxx
 *     Called by the hospital's browser (via AJAX polling) to get the
 *     collector's current location for a specific waste record.
 *     Returns: { latitude, longitude, lastUpdate, status, collectorName }
 *
 * This implements practical collector tracking without WebSockets:
 *   - Collector browser sends location every 15 seconds while EN_ROUTE/ARRIVED
 *   - Hospital browser polls every 15 seconds for the update
 *   - Tracking stops automatically once status moves past COLLECTED
 */
@WebServlet("/api/collectorLocation")
public class CollectorLocationServlet extends HttpServlet {

    // Statuses during which tracking is active
    private static final java.util.Set<String> TRACKING_STATUSES =
            new java.util.HashSet<>(java.util.Arrays.asList("ACCEPTED", "EN_ROUTE", "ARRIVED"));

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("collectorId") == null) {
            resp.sendError(403); return;
        }

        String wasteId   = req.getParameter("wasteId");
        String latStr    = req.getParameter("latitude");
        String lngStr    = req.getParameter("longitude");

        if (wasteId == null || latStr == null || lngStr == null) {
            resp.sendError(400, "wasteId, latitude, longitude required"); return;
        }

        double lat, lng;
        try { lat = Double.parseDouble(latStr); lng = Double.parseDouble(lngStr); }
        catch (NumberFormatException e) { resp.sendError(400, "Invalid coordinates"); return; }

        MongoDatabase db = MongoDBConnection.getDatabase();
        Document wasteDoc = db.getCollection("wasteRecords")
                .find(new Document("_id", new ObjectId(wasteId))).first();

        if (wasteDoc == null) { resp.sendError(404); return; }

        // Only update location if collection is still active
        String status = wasteDoc.getString("status");
        if (!TRACKING_STATUSES.contains(status)) {
            resp.setContentType("application/json");
            resp.getWriter().print("{\"tracking\":false}");
            return;
        }

        db.getCollection("wasteRecords").updateOne(
                new Document("_id", new ObjectId(wasteId)),
                new Document("$set", new Document("collectorLatitude", lat)
                        .append("collectorLongitude", lng)
                        .append("lastLocationUpdate", Instant.now().toString())));

        resp.setContentType("application/json");
        resp.getWriter().print("{\"tracking\":true}");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String wasteId = req.getParameter("wasteId");
        Map<String, Object> result = new HashMap<>();

        if (wasteId == null) { new Gson().toJson(result, resp.getWriter()); return; }

        try {
            MongoDatabase db = MongoDBConnection.getDatabase();
            Document wasteDoc = db.getCollection("wasteRecords")
                    .find(new Document("_id", new ObjectId(wasteId))).first();

            if (wasteDoc == null) { result.put("error", "not_found"); }
            else {
                String status = wasteDoc.getString("status");
                result.put("status", status);
                result.put("tracking", TRACKING_STATUSES.contains(status));

                Object lat = wasteDoc.get("collectorLatitude");
                Object lng = wasteDoc.get("collectorLongitude");
                if (lat != null && lng != null) {
                    double latD = ((Number) lat).doubleValue();
                    double lngD = ((Number) lng).doubleValue();
                    // Only return location if it's a real coordinate (not 0,0)
                    if (latD != 0 || lngD != 0) {
                        result.put("latitude", latD);
                        result.put("longitude", lngD);
                        result.put("lastLocationUpdate", wasteDoc.getString("lastLocationUpdate"));

                        // Get hospital coordinates for distance calculation
                        String hospitalId = wasteDoc.getString("hospitalId");
                        if (hospitalId != null) {
                            Document hospital = db.getCollection("hospitals")
                                    .find(new Document("_id", new ObjectId(hospitalId))).first();
                            if (hospital != null) {
                                double hLat = ((Number) hospital.get("latitude")).doubleValue();
                                double hLng = ((Number) hospital.get("longitude")).doubleValue();
                                // Haversine distance in km
                                double dist = haversine(latD, lngD, hLat, hLng);
                                result.put("distanceKm", Math.round(dist * 10.0) / 10.0);
                                // Rough ETA assuming 30 km/h average city speed
                                double etaMinutes = (dist / 30.0) * 60.0;
                                result.put("etaMinutes", (int) Math.ceil(etaMinutes));
                            }
                        }
                    }
                }

                // Include collector name
                String collectorId = wasteDoc.getString("assignedCollectorId");
                if (collectorId != null) {
                    Document collector = db.getCollection("collectors")
                            .find(new Document("_id", new ObjectId(collectorId))).first();
                    if (collector != null) result.put("collectorName", collector.getString("fullName"));
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        new Gson().toJson(result, resp.getWriter());
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
