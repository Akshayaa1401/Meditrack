package com.meditrack.servlet;

import com.meditrack.model.Hospital;
import com.meditrack.model.WasteRecord;
import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.NotificationUtil;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

/**
 * HospitalDashboardServlet
 * -------------------------
 * Shows:
 *   1. Hospital profile card
 *   2. In-app notifications (unread disposal confirmations shown as green banners)
 *   3. Status-count summary (Pending / Assigned / Collected / Disposed boxes)
 *   4. Full waste history table with collector names resolved (so staff see
 *      "Assigned to Ravi Kumar", not just a raw status code)
 */
@WebServlet("/hospital/dashboard")
public class HospitalDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String hospitalId = (String) session.getAttribute("hospitalId");
        String userId     = (String) session.getAttribute("userId");
        MongoDatabase db  = MongoDBConnection.getDatabase();

        if (hospitalId != null) {
            // Hospital profile
            Hospital hospital = Hospital.fromDocument(db.getCollection("hospitals")
                    .find(new Document("_id", new ObjectId(hospitalId))).first());
            req.setAttribute("hospital", hospital);

            // Waste records — most recent first
            FindIterable<Document> results = db.getCollection("wasteRecords")
                    .find(new Document("hospitalId", hospitalId))
                    .sort(new Document("emergency", -1).append("priorityScore", -1).append("reportedAt", -1));

            List<WasteRecord> records = new ArrayList<>();
            Set<String> collectorIds = new HashSet<>();
            for (Document d : results) {
                WasteRecord w = WasteRecord.fromDocument(d);
                records.add(w);
                if (w.getAssignedCollectorId() != null) collectorIds.add(w.getAssignedCollectorId());
            }
            req.setAttribute("wasteRecords", records);

            // Resolve collector names in one batch so JSP can show "Assigned to Ravi Kumar"
            Map<String, String> collectorNames = new HashMap<>();
            if (!collectorIds.isEmpty()) {
                List<ObjectId> oids = new ArrayList<>();
                for (String id : collectorIds) {
                    try { oids.add(new ObjectId(id)); } catch (Exception ignored) {}
                }
                for (Document c : db.getCollection("collectors")
                        .find(new Document("_id", new Document("$in", oids)))) {
                    collectorNames.put(c.getObjectId("_id").toHexString(), c.getString("fullName"));
                }
            }
            req.setAttribute("collectorNames", collectorNames);
        }

        // Fetch unread in-app notifications for this user (disposal confirmations etc.)
        if (userId != null) {
            List<Document> notifications = new ArrayList<>();
            for (Document n : db.getCollection("notifications")
                    .find(new Document("recipientUserId", userId).append("read", false))
                    .sort(new Document("createdAt", -1)).limit(10)) {
                notifications.add(n);
            }
            req.setAttribute("notifications", notifications);
            // Mark them read now they've been shown to the user
            NotificationUtil.markAllRead(db, userId);
        }

        req.getRequestDispatcher("/WEB-INF/views/hospital/dashboard.jsp").forward(req, resp);
    }
}
