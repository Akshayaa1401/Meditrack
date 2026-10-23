package com.meditrack.servlet;

import com.meditrack.model.CollectionHistory;
import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.NotificationUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * CollectorStatusUpdateServlet
 * -----------------------------
 * Handles the FULL 7-step waste collection lifecycle:
 *
 *   PENDING → ASSIGNED → ACCEPTED → EN_ROUTE → ARRIVED → COLLECTED → DISPOSED
 *
 * Also handles OVERDUE and CANCELLED transitions.
 * Invalid transitions are silently ignored (server-side guard).
 *
 * On each transition, a timestamp is recorded and an in-app notification is
 * sent to the hospital staff so they can track progress in real time.
 */
@WebServlet("/collector/updateStatus")
public class CollectorStatusUpdateServlet extends HttpServlet {

    // Valid transitions: what status can follow what
    private static boolean isValidTransition(String current, String next) {
        if (current == null) current = "PENDING";
        switch (current) {
            case "PENDING":   return "ASSIGNED".equals(next);
            case "ASSIGNED":  return "ACCEPTED".equals(next) || "CANCELLED".equals(next);
            case "ACCEPTED":  return "EN_ROUTE".equals(next) || "CANCELLED".equals(next);
            case "EN_ROUTE":  return "ARRIVED".equals(next);
            case "ARRIVED":   return "COLLECTED".equals(next);
            case "COLLECTED": return "DISPOSED".equals(next);
            case "OVERDUE":   return "ACCEPTED".equals(next) || "CANCELLED".equals(next); // allow re-accept after overdue
            default:          return false;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String collectorId   = (String) session.getAttribute("collectorId");
        String collectorName = (String) session.getAttribute("fullName");
        String wasteId       = req.getParameter("wasteId");
        String newStatus     = req.getParameter("status");
        String delayReason   = req.getParameter("delayReason"); // optional, for overdue records

        if (collectorId == null || wasteId == null || newStatus == null) {
            resp.sendRedirect(req.getContextPath() + "/collector/dashboard?error=invalid_request"); return;
        }

        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> wasteCol = db.getCollection("wasteRecords");
        Document wasteDoc = wasteCol.find(new Document("_id", new ObjectId(wasteId))).first();
        if (wasteDoc == null) {
            resp.sendRedirect(req.getContextPath() + "/collector/dashboard?error=not_found"); return;
        }

        String currentStatus = wasteDoc.getString("status");
        if (!isValidTransition(currentStatus, newStatus)) {
            resp.sendRedirect(req.getContextPath() + "/collector/dashboard?error=invalid_transition"); return;
        }

        String now = Instant.now().toString();

        // Build the update document — always set status + the matching timestamp
        Document setFields = new Document("status", newStatus);
        switch (newStatus) {
            case "ACCEPTED":  setFields.append("acceptedAt",    now); break;
            case "EN_ROUTE":  setFields.append("routeStartedAt", now); break;
            case "ARRIVED":   setFields.append("arrivedAt",     now); break;
            case "COLLECTED": setFields.append("collectedAt",   now); break;
            case "DISPOSED":  setFields.append("disposedAt",    now); break;
            case "CANCELLED": setFields.append("cancelledAt",   now); break;
        }
        if (delayReason != null && !delayReason.trim().isEmpty()) {
            setFields.append("delayReason", delayReason);
        }

        wasteCol.updateOne(new Document("_id", new ObjectId(wasteId)), new Document("$set", setFields));

        // Find hospital staff user for notifications
        String hospitalId = wasteDoc.getString("hospitalId");
        String staffUserId = findStaffUserId(db, hospitalId);
        String wasteType   = wasteDoc.getString("wasteType");
        double qty         = wasteDoc.get("quantityKg") != null ? ((Number) wasteDoc.get("quantityKg")).doubleValue() : 0;
        String hospitalName = wasteDoc.getString("hospitalName");

        // Send notification to hospital based on transition
        if (staffUserId != null) {
            String msg = null;
            switch (newStatus) {
                case "ACCEPTED":
                    msg = "✅ Collector " + collectorName + " has accepted your " + wasteType + " collection request.";
                    break;
                case "EN_ROUTE":
                    msg = "🚚 Collector " + collectorName + " is on the way to collect your " + wasteType + " waste (" + qty + " kg).";
                    break;
                case "ARRIVED":
                    msg = "📍 Collector " + collectorName + " has arrived at " + hospitalName + ". Please have the waste ready.";
                    break;
                case "COLLECTED":
                    msg = "📦 Your " + wasteType + " waste (" + qty + " kg) has been collected and is being transported.";
                    break;
                case "DISPOSED":
                    msg = "✓ Your " + wasteType + " waste (" + qty + " kg) has been safely disposed of" +
                          (collectorName != null ? " by " + collectorName : "") + ".";
                    break;
            }
            if (msg != null) NotificationUtil.send(db, staffUserId, newStatus, msg, wasteId);
        }

        // Find all admin users for notifications on key events
        if ("DISPOSED".equals(newStatus) || "EN_ROUTE".equals(newStatus) || "COLLECTED".equals(newStatus)) {
            for (Document admin : db.getCollection("users").find(new Document("role", "ADMIN"))) {
                String adminId = admin.getObjectId("_id").toHexString();
                String adminMsg = "Collection " + newStatus.replace("_"," ") + ": " + wasteType +
                        " at " + hospitalName + " (" + qty + " kg) — by " + collectorName;
                NotificationUtil.send(db, adminId, newStatus, adminMsg, wasteId);
            }
        }

        // When COLLECTED: remove from active route so map stays accurate
        if ("COLLECTED".equals(newStatus)) {
            MongoCollection<Document> assignmentsCol = db.getCollection("collectionAssignments");
            Document active = assignmentsCol.find(
                    new Document("collectorId", collectorId).append("status", "ACTIVE")).first();
            if (active != null) {
                @SuppressWarnings("unchecked")
                List<Document> route = (List<Document>) active.get("route");
                List<Document> updatedRoute = new ArrayList<>();
                if (route != null) for (Document s : route)
                    if (!wasteId.equals(s.getString("wasteId"))) updatedRoute.add(s);
                assignmentsCol.updateOne(new Document("_id", active.getObjectId("_id")),
                        new Document("$set", new Document("route", updatedRoute).append("updatedAt", now)));
            }
        }

        // When DISPOSED: write permanent audit record + increment counter
        if ("DISPOSED".equals(newStatus)) {
            CollectionHistory history = new CollectionHistory();
            history.setWasteId(wasteId);
            history.setHospitalId(wasteDoc.getString("hospitalId"));
            history.setHospitalName(wasteDoc.getString("hospitalName"));
            history.setCollectorId(collectorId);
            history.setWasteType(wasteType);
            history.setQuantityKg(qty);
            history.setRiskLevel(wasteDoc.getString("riskLevel"));
            history.setCollectedAt(wasteDoc.getString("collectedAt"));
            history.setDisposedAt(now);
            history.setPriorityScoreAtCollection(
                    wasteDoc.get("priorityScore") != null ? ((Number) wasteDoc.get("priorityScore")).doubleValue() : 0);
            db.getCollection("collectionHistory").insertOne(history.toDocument());
            db.getCollection("collectors").updateOne(new Document("_id", new ObjectId(collectorId)),
                    new Document("$inc", new Document("totalCollections", 1)));
        }

        resp.sendRedirect(req.getContextPath() + "/collector/dashboard?updated=true&status=" + newStatus);
    }

    static String findStaffUserId(MongoDatabase db, String hospitalId) {
        if (hospitalId == null) return null;
        try {
            Document hospital = db.getCollection("hospitals")
                    .find(new Document("_id", new ObjectId(hospitalId))).first();
            if (hospital != null) {
                String byUserId = hospital.getString("registeredByUserId");
                if (byUserId != null) return byUserId;
            }
            Document staffUser = db.getCollection("users")
                    .find(new Document("hospitalId", hospitalId)).first();
            return staffUser != null ? staffUser.getObjectId("_id").toHexString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
