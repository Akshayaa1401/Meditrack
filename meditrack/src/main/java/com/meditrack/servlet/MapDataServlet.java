package com.meditrack.servlet;

import com.google.gson.Gson;
import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * MapDataServlet (updated)
 * -------------------------
 * JSON API for all Leaflet maps. Now includes:
 *   - hospitals (all registered)
 *   - wastePoints (PENDING, ASSIGNED, ACCEPTED, EN_ROUTE, ARRIVED, OVERDUE)
 *   - activeRoute (for a specific collector)
 *   - collectorLocations (for admin live map — all active collectors with GPS)
 *
 * GET /api/mapData?collectorId=xxx   (collectorId optional)
 * GET /api/mapData?adminView=true    (returns all active collector locations)
 */
@WebServlet("/api/mapData")
public class MapDataServlet extends HttpServlet {

    private static final List<String> ACTIVE_STATUSES = Arrays.asList(
            "PENDING", "ASSIGNED", "ACCEPTED", "EN_ROUTE", "ARRIVED", "OVERDUE");

    private static boolean isValidCoord(Object lat, Object lng) {
        if (lat == null || lng == null) return false;
        double la = ((Number) lat).doubleValue();
        double ln = ((Number) lng).doubleValue();
        return la >= -90 && la <= 90 && ln >= -180 && ln <= 180 && !(la == 0 && ln == 0);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        MongoDatabase db = MongoDBConnection.getDatabase();
        Map<String, Object> result = new HashMap<>();

        // ── Hospitals ─────────────────────────────────────────────────────────
        List<Map<String, Object>> hospitals = new ArrayList<>();
        MongoCollection<Document> hospitalsCol = db.getCollection("hospitals");
        for (Document h : hospitalsCol.find()) {
            Object lat = h.get("latitude"), lng = h.get("longitude");
            if (!isValidCoord(lat, lng)) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id",        h.getObjectId("_id").toHexString());
            m.put("name",      h.getString("name"));
            m.put("address",   h.getString("address"));
            m.put("latitude",  lat);
            m.put("longitude", lng);
            hospitals.add(m);
        }
        result.put("hospitals", hospitals);

        // ── Active waste points (all active statuses, joined with hospital coords) ─
        List<Map<String, Object>> wastePoints = new ArrayList<>();
        MongoCollection<Document> wasteCol = db.getCollection("wasteRecords");
        for (Document w : wasteCol.find(new Document("status", new Document("$in", ACTIVE_STATUSES)))) {
            String hospitalId = w.getString("hospitalId");
            if (hospitalId == null) continue;
            Document h;
            try { h = hospitalsCol.find(new Document("_id", new ObjectId(hospitalId))).first(); }
            catch (Exception e) { continue; }
            if (h == null) continue;

            Object lat = h.get("latitude"), lng = h.get("longitude");
            if (!isValidCoord(lat, lng)) continue;

            Map<String, Object> m = new HashMap<>();
            m.put("wasteId",      w.getObjectId("_id").toHexString());
            m.put("hospitalName", w.getString("hospitalName"));
            m.put("wasteType",    w.getString("wasteType"));
            m.put("quantityKg",   w.get("quantityKg"));
            m.put("riskLevel",    w.getString("riskLevel"));
            m.put("status",       w.getString("status"));
            m.put("priorityScore", w.get("priorityScore"));
            m.put("emergency",    w.getBoolean("emergency", false));
            m.put("overdue",      w.getBoolean("overdue", false));
            m.put("deadline",     w.getString("deadline"));
            m.put("latitude",     lat);
            m.put("longitude",    lng);

            // Collector location (if tracking is active)
            Object cLat = w.get("collectorLatitude");
            Object cLng = w.get("collectorLongitude");
            if (cLat != null && cLng != null) {
                double cLatD = ((Number) cLat).doubleValue();
                double cLngD = ((Number) cLng).doubleValue();
                if (cLatD != 0 || cLngD != 0) {
                    m.put("collectorLatitude",     cLatD);
                    m.put("collectorLongitude",    cLng);
                    m.put("lastLocationUpdate",    w.getString("lastLocationUpdate"));
                }
            }
            wastePoints.add(m);
        }
        result.put("wastePoints", wastePoints);

        // ── Active route for a specific collector ─────────────────────────────
        String collectorId = req.getParameter("collectorId");
        if (collectorId != null) {
            Document active = db.getCollection("collectionAssignments")
                    .find(new Document("collectorId", collectorId).append("status", "ACTIVE")).first();
            if (active != null) {
                result.put("activeRoute",      active.get("route"));
                result.put("totalDistanceKm",  active.get("totalDistanceKm"));
            }
        }

        // ── All active collector locations (for admin map) ─────────────────────
        if ("true".equals(req.getParameter("adminView"))) {
            List<Map<String, Object>> collectorLocations = new ArrayList<>();
            for (Document c : db.getCollection("collectors").find()) {
                Object lat = c.get("currentLat"), lng = c.get("currentLng");
                String status = c.getString("status");
                if (lat == null || lng == null) continue;
                double latD = ((Number) lat).doubleValue();
                double lngD = ((Number) lng).doubleValue();
                if (latD == 0 && lngD == 0) continue; // skip un-set coordinates
                Map<String, Object> m = new HashMap<>();
                m.put("id",          c.getObjectId("_id").toHexString());
                m.put("name",        c.getString("fullName"));
                m.put("status",      status);
                m.put("latitude",    latD);
                m.put("longitude",   lngD);
                collectorLocations.add(m);
            }
            result.put("collectorLocations", collectorLocations);
        }

        try (PrintWriter out = resp.getWriter()) {
            out.print(new Gson().toJson(result));
        }
    }
}
