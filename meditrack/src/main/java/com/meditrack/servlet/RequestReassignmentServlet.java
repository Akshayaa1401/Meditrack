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
 * RequestReassignmentServlet
 * ---------------------------
 * Hospital staff can request a reassignment for an OVERDUE waste record.
 * Sets a flag "reassignmentRequested=true" and notifies all admins.
 * Admins then use AdminReassignServlet to actually assign a new collector.
 * Original collector and delay history are preserved.
 *
 * POST /hospital/requestReassignment
 *   Params: wasteId
 */
@WebServlet("/hospital/requestReassignment")
public class RequestReassignmentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || !"HOSPITAL_STAFF".equals(session.getAttribute("role"))) {
            resp.sendError(403); return;
        }

        String wasteId    = req.getParameter("wasteId");
        String hospitalId = (String) session.getAttribute("hospitalId");

        if (wasteId == null || hospitalId == null) {
            resp.sendRedirect(req.getContextPath() + "/hospital/dashboard?error=invalid"); return;
        }

        MongoDatabase db = MongoDBConnection.getDatabase();
        Document waste = db.getCollection("wasteRecords")
                .find(new Document("_id", new ObjectId(wasteId))
                        .append("hospitalId", hospitalId)).first();

        if (waste == null) {
            resp.sendRedirect(req.getContextPath() + "/hospital/dashboard?error=not_found"); return;
        }

        // Allow for OVERDUE records OR emergency records not yet accepted
        String status    = waste.getString("status");
        boolean emergency = waste.getBoolean("emergency", false);
        boolean canEscalate = "OVERDUE".equals(status)
                || (emergency && ("PENDING".equals(status) || "ASSIGNED".equals(status)));
        if (!canEscalate) {
            resp.sendRedirect(req.getContextPath() + "/hospital/dashboard?error=cannot_escalate"); return;
        }

        // Mark reassignment requested
        db.getCollection("wasteRecords").updateOne(
                new Document("_id", new ObjectId(wasteId)),
                new Document("$set", new Document("reassignmentRequested", true)
                        .append("reassignmentRequestedAt", Instant.now().toString())));

        // Notify all admins
        String hospitalName = waste.getString("hospitalName");
        String wasteType    = waste.getString("wasteType");
        String msg = "⚠️ Reassignment requested by " + hospitalName +
                     " for OVERDUE " + wasteType + " collection. Please assign a new collector.";

        for (Document admin : db.getCollection("users").find(new Document("role", "ADMIN"))) {
            NotificationUtil.send(db, admin.getObjectId("_id").toHexString(),
                    "REASSIGNMENT_REQUESTED", msg, wasteId);
        }

        resp.sendRedirect(req.getContextPath() + "/hospital/dashboard?reassignRequested=true");
    }
}
