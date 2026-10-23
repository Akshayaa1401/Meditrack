package com.meditrack.servlet;

import com.meditrack.model.Collector;
import com.meditrack.model.RouteUpdate;
import com.meditrack.model.WasteRecord;
import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        MongoDatabase db = MongoDBConnection.getDatabase();
        String todayPrefix = LocalDate.now(ZoneOffset.UTC).toString();

        // ── Core counts ──────────────────────────────────────────────────────────
        req.setAttribute("totalHospitals",  db.getCollection("hospitals").countDocuments());
        req.setAttribute("totalCollectors", db.getCollection("collectors").countDocuments());
        req.setAttribute("pendingWaste",
                db.getCollection("wasteRecords").countDocuments(new Document("status", "PENDING")));
        req.setAttribute("assignedWaste",
                db.getCollection("wasteRecords").countDocuments(
                        new Document("status", new Document("$in", Arrays.asList("ASSIGNED","ACCEPTED","EN_ROUTE","ARRIVED")))));
        req.setAttribute("collectedWaste",
                db.getCollection("wasteRecords").countDocuments(new Document("status", "COLLECTED")));
        req.setAttribute("disposedWaste",
                db.getCollection("wasteRecords").countDocuments(new Document("status", "DISPOSED")));
        req.setAttribute("emergencyCount",
                db.getCollection("wasteRecords").countDocuments(
                        new Document("emergency", true)
                                .append("status", new Document("$nin", Arrays.asList("DISPOSED","CANCELLED")))));
        req.setAttribute("overdueCount",
                db.getCollection("wasteRecords").countDocuments(new Document("status", "OVERDUE")));

        // ── Today's stats ────────────────────────────────────────────────────────
        long collectedToday = 0;
        double kgCollectedToday = 0;
        for (Document d : db.getCollection("wasteRecords")
                .find(new Document("status", new Document("$in", Arrays.asList("COLLECTED","DISPOSED"))))) {
            String ts = d.getString("collectedAt");
            if (ts != null && ts.startsWith(todayPrefix)) {
                collectedToday++;
                Object qty = d.get("quantityKg");
                if (qty != null) kgCollectedToday += ((Number) qty).doubleValue();
            }
        }
        req.setAttribute("collectedToday",   collectedToday);
        req.setAttribute("kgCollectedToday", Math.round(kgCollectedToday * 10.0) / 10.0);

        // ── Emergency records (not disposed) ─────────────────────────────────────
        List<WasteRecord> emergencyRecords = new ArrayList<>();
        for (Document d : db.getCollection("wasteRecords")
                .find(new Document("emergency", true)
                        .append("status", new Document("$nin", Arrays.asList("DISPOSED","CANCELLED"))))
                .sort(new Document("priorityScore", -1))) {
            emergencyRecords.add(WasteRecord.fromDocument(d));
        }
        req.setAttribute("emergencyRecords", emergencyRecords);

        // ── Overdue records ───────────────────────────────────────────────────────
        List<WasteRecord> overdueRecords = new ArrayList<>();
        for (Document d : db.getCollection("wasteRecords")
                .find(new Document("status", "OVERDUE"))
                .sort(new Document("deadline", 1))) {
            overdueRecords.add(WasteRecord.fromDocument(d));
        }
        req.setAttribute("overdueRecords", overdueRecords);

        // ── Available collectors for reassignment ─────────────────────────────────
        List<Collector> availableCollectors = new ArrayList<>();
        for (Document c : db.getCollection("collectors")
                .find(new Document("status", new Document("$in", Arrays.asList("AVAILABLE","ON_ROUTE"))))) {
            availableCollectors.add(Collector.fromDocument(c));
        }
        req.setAttribute("availableCollectors", availableCollectors);

        // ── Recent route re-optimization activity ─────────────────────────────────
        List<RouteUpdate> recentUpdates = new ArrayList<>();
        for (Document d : db.getCollection("routeUpdates")
                .find().sort(new Document("createdAt", -1)).limit(10)) {
            recentUpdates.add(RouteUpdate.fromDocument(d));
        }
        req.setAttribute("recentUpdates", recentUpdates);

        // ── Unread notifications for this admin ───────────────────────────────────
        String userId = (String) req.getSession().getAttribute("userId");
        if (userId != null) {
            long unreadCount = db.getCollection("notifications")
                    .countDocuments(new Document("recipientUserId", userId).append("read", false));
            req.setAttribute("unreadCount", unreadCount);
            List<Document> notifications = new ArrayList<>();
            for (Document n : db.getCollection("notifications")
                    .find(new Document("recipientUserId", userId).append("read", false))
                    .sort(new Document("createdAt", -1)).limit(10)) {
                notifications.add(n);
            }
            req.setAttribute("notifications", notifications);
        }

        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
    }
}
