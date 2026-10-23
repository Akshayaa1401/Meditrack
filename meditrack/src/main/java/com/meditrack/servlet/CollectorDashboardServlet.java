package com.meditrack.servlet;

import com.meditrack.model.CollectionAssignment;
import com.meditrack.model.RouteUpdate;
import com.meditrack.model.WasteRecord;
import com.meditrack.util.DeadlineConfig;
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
import java.util.List;

/**
 * CollectorDashboardServlet
 * --------------------------
 * Passes WasteRecord POJOs (not raw Documents) to JSP so EL expressions
 * like ${w.id}, ${w.status}, ${w.hospitalName} work correctly via getters.
 *
 * Three clearly separated sections:
 *   1. ACTIVE      — ASSIGNED/ACCEPTED/EN_ROUTE/ARRIVED records for this collector
 *   2. OVERDUE     — OVERDUE records requiring immediate attention
 *   3. TODAY       — COLLECTED/DISPOSED completed today
 *   4. HISTORY     — All past completed records (last 20)
 */
@WebServlet("/collector/dashboard")
public class CollectorDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session    = req.getSession();
        String collectorId     = (String) session.getAttribute("collectorId");
        String userId          = (String) session.getAttribute("userId");
        MongoDatabase db       = MongoDBConnection.getDatabase();

        if (collectorId != null) {
            // ── Active route assignment ──────────────────────────────────────
            Document activeDoc = db.getCollection("collectionAssignments")
                    .find(new Document("collectorId", collectorId)
                            .append("status", "ACTIVE")).first();
            req.setAttribute("assignment", CollectionAssignment.fromDocument(activeDoc));

            // ── Overdue records ──────────────────────────────────────────────
            List<WasteRecord> overdueWaste = new ArrayList<>();
            for (Document d : db.getCollection("wasteRecords")
                    .find(new Document("assignedCollectorId", collectorId)
                            .append("status", "OVERDUE"))) {
                overdueWaste.add(WasteRecord.fromDocument(d));
            }
            req.setAttribute("overdueWasteRecords", overdueWaste);

            // ── Active waste records (in-progress) ───────────────────────────
            List<String> activeStatuses = List.of("ASSIGNED","ACCEPTED","EN_ROUTE","ARRIVED");
            List<WasteRecord> activeWaste = new ArrayList<>();
            for (Document d : db.getCollection("wasteRecords")
                    .find(new Document("assignedCollectorId", collectorId)
                            .append("status", new Document("$in", activeStatuses)))
                    .sort(new Document("emergency", -1).append("priorityScore", -1).append("deadline", 1))) {
                WasteRecord w = WasteRecord.fromDocument(d);
                // pre-compute deadline label (avoids EL logic in JSP)
                w.setDescription(w.getDescription()); // no-op, just ensure non-null
                activeWaste.add(w);
            }
            req.setAttribute("activeWasteRecords", activeWaste);

            // ── Completed today ──────────────────────────────────────────────
            String todayPrefix = LocalDate.now(ZoneOffset.UTC).toString();
            List<WasteRecord> completedToday = new ArrayList<>();
            double totalKgToday = 0;
            for (Document d : db.getCollection("wasteRecords")
                    .find(new Document("assignedCollectorId", collectorId)
                            .append("status", new Document("$in", List.of("COLLECTED","DISPOSED"))))
                    .sort(new Document("collectedAt", -1))) {
                WasteRecord w = WasteRecord.fromDocument(d);
                String collectedAt = w.getCollectedAt();
                if (collectedAt != null && collectedAt.startsWith(todayPrefix)) {
                    completedToday.add(w);
                    totalKgToday += w.getQuantityKg();
                }
            }
            req.setAttribute("completedToday", completedToday);
            req.setAttribute("totalKgToday", Math.round(totalKgToday * 10.0) / 10.0);

            // ── Collection history (paginated, 10/page, excluding today) ─────────
            final int PAGE_SIZE = 10;
            int histPage = 1;
            try {
                String hp = req.getParameter("historyPage");
                if (hp != null) histPage = Math.max(1, Integer.parseInt(hp));
            } catch (NumberFormatException ignored) {}

            // Count total (excluding today)
            long totalHistoryRaw = db.getCollection("wasteRecords").countDocuments(
                    new Document("assignedCollectorId", collectorId)
                            .append("status", new Document("$in", List.of("COLLECTED","DISPOSED"))));
            // Approximate total pages (today records are a small fraction)
            int totalHistPages = (int) Math.max(1, Math.ceil((double)(totalHistoryRaw - completedToday.size()) / PAGE_SIZE));
            histPage = Math.min(histPage, totalHistPages);

            int skip = (histPage - 1) * PAGE_SIZE;
            List<WasteRecord> history = new ArrayList<>();
            int skipped = 0;
            for (Document d : db.getCollection("wasteRecords")
                    .find(new Document("assignedCollectorId", collectorId)
                            .append("status", new Document("$in", List.of("COLLECTED","DISPOSED"))))
                    .sort(new Document("collectedAt", -1).append("disposedAt", -1))
                    .limit(skip + PAGE_SIZE + completedToday.size() + 5)) {
                WasteRecord w = WasteRecord.fromDocument(d);
                String collectedAt = w.getCollectedAt();
                if (collectedAt != null && collectedAt.startsWith(todayPrefix)) continue; // skip today
                if (skipped < skip) { skipped++; continue; }
                if (history.size() < PAGE_SIZE) history.add(w);
            }
            req.setAttribute("collectionHistory",  history);
            req.setAttribute("historyPage",         histPage);
            req.setAttribute("historyTotalPages",   totalHistPages);
            req.setAttribute("historyTotal",        Math.max(0, totalHistoryRaw - completedToday.size()));

            // ── Latest route re-opt event ─────────────────────────────────────
            Document latestUpdateDoc = db.getCollection("routeUpdates")
                    .find(new Document("collectorId", collectorId))
                    .sort(new Document("createdAt", -1)).first();
            req.setAttribute("latestRouteUpdate", RouteUpdate.fromDocument(latestUpdateDoc));

            // ── Unread notifications ──────────────────────────────────────────
            if (userId != null) {
                List<Document> notifications = new ArrayList<>();
                for (Document n : db.getCollection("notifications")
                        .find(new Document("recipientUserId", userId)
                                .append("read", false))
                        .sort(new Document("createdAt", -1)).limit(10)) {
                    notifications.add(n);
                }
                req.setAttribute("notifications", notifications);
                long unreadCount = db.getCollection("notifications")
                        .countDocuments(new Document("recipientUserId", userId)
                                .append("read", false));
                req.setAttribute("unreadCount", unreadCount);
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/collector/dashboard.jsp")
                .forward(req, resp);
    }
}
