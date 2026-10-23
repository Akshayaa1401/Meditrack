package com.meditrack.servlet;

import com.meditrack.model.Hospital;
import com.meditrack.model.WasteRecord;
import com.meditrack.util.DeadlineConfig;
import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.NotificationUtil;
import com.meditrack.util.PriorityCalculator;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.Instant;

@WebServlet("/hospital/submitWaste")
public class WasteSubmitServlet extends HttpServlet {

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/hospital/submitWaste.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String hospitalId = (String) session.getAttribute("hospitalId");
        if (hospitalId == null) {
            req.setAttribute("error", "Please register your hospital profile first.");
            req.getRequestDispatcher("/WEB-INF/views/hospital/submitWaste.jsp").forward(req, resp); return;
        }
        String wasteType    = req.getParameter("wasteType");
        String quantityStr  = req.getParameter("quantityKg");
        String description  = req.getParameter("description");

        if (wasteType == null || quantityStr == null || wasteType.trim().isEmpty()) {
            req.setAttribute("error", "Waste type and quantity are required.");
            req.getRequestDispatcher("/WEB-INF/views/hospital/submitWaste.jsp").forward(req, resp); return;
        }
        double quantityKg;
        try { quantityKg = Double.parseDouble(quantityStr); }
        catch (NumberFormatException e) {
            req.setAttribute("error", "Quantity must be a valid number.");
            req.getRequestDispatcher("/WEB-INF/views/hospital/submitWaste.jsp").forward(req, resp); return;
        }

        String riskLevel = getRiskLevelForType(wasteType);

        MongoDatabase db = MongoDBConnection.getDatabase();
        Hospital hospital = Hospital.fromDocument(db.getCollection("hospitals")
                .find(new Document("_id", new ObjectId(hospitalId))).first());

        Instant now = Instant.now();
        double priority = PriorityCalculator.calculatePriority(riskLevel, quantityKg, now);
        boolean emergency = PriorityCalculator.isEmergency(riskLevel, quantityKg, now);
        String deadline = DeadlineConfig.calculateDeadline(riskLevel, emergency);

        WasteRecord record = new WasteRecord();
        record.setHospitalId(hospitalId);
        record.setHospitalName(hospital != null ? hospital.getName() : "Unknown");
        record.setWasteType(wasteType);
        record.setQuantityKg(quantityKg);
        record.setRiskLevel(riskLevel);
        record.setDescription(description);
        record.setReportedAt(now.toString());
        record.setStatus("PENDING");
        record.setPriorityScore(priority);
        record.setEmergency(emergency);
        record.setDeadline(deadline);
        record.setOverdue(false);

        Document doc = record.toDocument();
        db.getCollection("wasteRecords").insertOne(doc);
        String wasteId = doc.getObjectId("_id").toHexString();

        // Notify all collectors of new request (especially important for EMERGENCY)
        String collectorMsg = emergency
                ? "🚨 EMERGENCY: New " + riskLevel + " risk " + wasteType + " collection at " + record.getHospitalName() + " — immediate response required!"
                : "📋 New collection request: " + wasteType + " (" + quantityKg + " kg, " + riskLevel + " risk) at " + record.getHospitalName();
        for (Document collector : db.getCollection("collectors").find()) {
            String cUserId = collector.getString("userId");
            if (cUserId != null) {
                NotificationUtil.send(db, cUserId, emergency ? "EMERGENCY" : "NEW_REQUEST", collectorMsg, wasteId);
            }
        }

        // Notify all admins
        String adminMsg = emergency
                ? "🚨 EMERGENCY waste reported: " + wasteType + " at " + record.getHospitalName() + " (" + quantityKg + " kg)"
                : "New waste request: " + wasteType + " at " + record.getHospitalName() + " (Priority: " + priority + ")";
        for (Document admin : db.getCollection("users").find(new Document("role", "ADMIN"))) {
            NotificationUtil.send(db, admin.getObjectId("_id").toHexString(),
                    emergency ? "EMERGENCY" : "NEW_REQUEST", adminMsg, wasteId);
        }

        if (emergency) EmergencyReoptimizeServlet.reoptimizeForEmergency(db, wasteId);

        resp.sendRedirect(req.getContextPath() + "/hospital/dashboard?submitted=true");
    }

    public static String getRiskLevelForType(String wasteType) {
        if (wasteType == null) return "LOW";
        switch (wasteType) {
            case "Sharps": case "Infectious": case "Pathological": return "HIGH";
            case "Pharmaceutical": case "Chemical":                return "MEDIUM";
            default:                                               return "LOW";
        }
    }
}
