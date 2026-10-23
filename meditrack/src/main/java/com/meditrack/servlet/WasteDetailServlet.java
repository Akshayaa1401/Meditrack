package com.meditrack.servlet;

import com.meditrack.model.WasteRecord;
import com.meditrack.util.DeadlineConfig;
import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * WasteDetailServlet
 * -------------------
 * Shows the full waste journey timeline for a single waste record.
 * Accessible by the hospital that reported it, or any admin.
 *
 * GET /waste/detail?id=wasteId
 */
@WebServlet("/waste/detail")
public class WasteDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null) { resp.sendRedirect(req.getContextPath() + "/login.jsp"); return; }

        String role    = (String) session.getAttribute("role");
        String userId  = (String) session.getAttribute("userId");
        String wasteId = req.getParameter("id");

        if (wasteId == null) { resp.sendRedirect(req.getContextPath() + "/hospital/dashboard"); return; }

        MongoDatabase db = MongoDBConnection.getDatabase();
        Document wasteDoc = db.getCollection("wasteRecords")
                .find(new Document("_id", new ObjectId(wasteId))).first();

        if (wasteDoc == null) { resp.sendError(404, "Waste record not found"); return; }

        // Access control: hospital can only see their own records
        if ("HOSPITAL_STAFF".equals(role)) {
            String hospitalId = (String) session.getAttribute("hospitalId");
            if (hospitalId == null || !hospitalId.equals(wasteDoc.getString("hospitalId"))) {
                resp.sendError(403, "Access denied"); return;
            }
        }

        WasteRecord record = WasteRecord.fromDocument(wasteDoc);
        req.setAttribute("waste", record);

        // Deadline status
        req.setAttribute("remainingTime", DeadlineConfig.remainingTimeLabel(record.getDeadline()));
        req.setAttribute("deadlineStatusClass", DeadlineConfig.deadlineStatusClass(record.getReportedAt(), record.getDeadline()));

        // Collector info if assigned
        if (record.getAssignedCollectorId() != null) {
            try {
                Document collector = db.getCollection("collectors")
                        .find(new Document("_id", new ObjectId(record.getAssignedCollectorId()))).first();
                req.setAttribute("collector", collector);
            } catch (Exception ignored) {}
        }

        // Hospital info
        if (record.getHospitalId() != null) {
            try {
                Document hospital = db.getCollection("hospitals")
                        .find(new Document("_id", new ObjectId(record.getHospitalId()))).first();
                req.setAttribute("hospital", hospital);
            } catch (Exception ignored) {}
        }

        req.setAttribute("trackingActive",
                record.getStatus() != null && (record.getStatus().equals("ACCEPTED") ||
                record.getStatus().equals("EN_ROUTE") || record.getStatus().equals("ARRIVED")));

        req.getRequestDispatcher("/WEB-INF/views/waste/detail.jsp").forward(req, resp);
    }
}
