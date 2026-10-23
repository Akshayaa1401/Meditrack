package com.meditrack.servlet;

import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.NotificationUtil;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.Instant;

/**
 * AdminReassignServlet
 * ---------------------
 * Allows an admin to reassign an OVERDUE or stuck waste record to a
 * different available collector. The original collector and delay are
 * preserved in the database for audit purposes.
 *
 * POST /admin/reassign
 *   Params: wasteId, newCollectorId, reason (optional)
 */
@WebServlet("/admin/reassign")
public class AdminReassignServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            resp.sendError(403); return;
        }

        String wasteId        = req.getParameter("wasteId");
        String newCollectorId = req.getParameter("newCollectorId");
        String reason         = req.getParameter("reason");

        if (wasteId == null || wasteId.trim().isEmpty()
                || newCollectorId == null || newCollectorId.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=select_collector"); return;
        }

        MongoDatabase db = MongoDBConnection.getDatabase();

        Document wasteDoc;
        Document newCollector;
        try {
            wasteDoc = db.getCollection("wasteRecords")
                    .find(new Document("_id", new ObjectId(wasteId))).first();
            newCollector = db.getCollection("collectors")
                    .find(new Document("_id", new ObjectId(newCollectorId))).first();
        } catch (IllegalArgumentException e) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=invalid_id"); return;
        }
        if (wasteDoc == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=not_found"); return;
        }
        if (newCollector == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=collector_not_found"); return;
        }

        String now = Instant.now().toString();
        String prevCollectorId = wasteDoc.getString("assignedCollectorId");

        // Update waste record: reassign and reset to ASSIGNED (new collector needs to ACCEPT)
        Document update = new Document("assignedCollectorId", newCollectorId)
                .append("status", "ASSIGNED")
                .append("assignedAt", now)
                .append("overdue", true) // keep overdue flag for audit
                .append("delayReason", reason != null ? reason : "Admin reassignment");
        if (prevCollectorId != null) update.append("previousCollectorId", prevCollectorId);

        db.getCollection("wasteRecords").updateOne(
                new Document("_id", new ObjectId(wasteId)),
                new Document("$set", update));

        // Notify new collector
        String newCollectorUserId = newCollector.getString("userId");
        if (newCollectorUserId != null) {
            NotificationUtil.send(db, newCollectorUserId, "REASSIGNED",
                    "🔴 You have been assigned an OVERDUE collection: " + wasteDoc.getString("wasteType") +
                    " at " + wasteDoc.getString("hospitalName") + " — immediate response required.", wasteId);
        }

        // Notify hospital
        String staffId = CollectorStatusUpdateServlet.findStaffUserId(db, wasteDoc.getString("hospitalId"));
        if (staffId != null) {
            NotificationUtil.send(db, staffId, "REASSIGNED",
                    "Your " + wasteDoc.getString("wasteType") + " collection has been reassigned to " +
                    newCollector.getString("fullName") + " by an administrator.", wasteId);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/dashboard?reassigned=true");
    }
}
