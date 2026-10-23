package com.meditrack.servlet;

import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

/**
 * AnalyticsServlet
 * ------------------
 * Builds aggregate analytics for the admin Analytics Dashboard:
 *   - total waste collected (kg) overall
 *   - waste category (wasteType) distribution
 *   - high-risk waste statistics
 *   - monthly collection report (group by year-month of disposedAt)
 *   - collector performance (totalCollections per collector)
 *
 * Uses MongoDB's aggregation pipeline for all the heavy lifting.
 */
@WebServlet("/admin/analytics")
public class AnalyticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> historyCol = db.getCollection("collectionHistory");

        // Total waste collected (kg)
        double totalKg = 0;
        for (Document d : historyCol.find()) {
            Object q = d.get("quantityKg");
            if (q != null) totalKg += ((Number) q).doubleValue();
        }
        req.setAttribute("totalKgCollected", Math.round(totalKg * 100.0) / 100.0);
        req.setAttribute("totalDisposalsCount", historyCol.countDocuments());

        // Waste category distribution
        List<Document> categoryPipeline = Arrays.asList(
                new Document("$group", new Document("_id", "$wasteType")
                        .append("totalKg", new Document("$sum", "$quantityKg"))
                        .append("count", new Document("$sum", 1))),
                new Document("$sort", new Document("totalKg", -1))
        );
        List<Document> categoryDistribution = new ArrayList<>();
        for (Document d : historyCol.aggregate(categoryPipeline)) categoryDistribution.add(d);
        req.setAttribute("categoryDistribution", categoryDistribution);

        // High-risk waste statistics
        long highRiskCount = historyCol.countDocuments(new Document("riskLevel", "HIGH"));
        double highRiskKg = 0;
        for (Document d : historyCol.find(new Document("riskLevel", "HIGH"))) {
            Object q = d.get("quantityKg");
            if (q != null) highRiskKg += ((Number) q).doubleValue();
        }
        req.setAttribute("highRiskCount", highRiskCount);
        req.setAttribute("highRiskKg", Math.round(highRiskKg * 100.0) / 100.0);

        // Monthly collection report - group by YYYY-MM substring of disposedAt (ISO-8601)
        List<Document> monthlyPipeline = Arrays.asList(
                new Document("$addFields", new Document("yearMonth",
                        new Document("$substrCP", Arrays.asList("$disposedAt", 0, 7)))),
                new Document("$group", new Document("_id", "$yearMonth")
                        .append("totalKg", new Document("$sum", "$quantityKg"))
                        .append("count", new Document("$sum", 1))),
                new Document("$sort", new Document("_id", 1))
        );
        List<Document> monthlyReport = new ArrayList<>();
        for (Document d : historyCol.aggregate(monthlyPipeline)) monthlyReport.add(d);
        req.setAttribute("monthlyReport", monthlyReport);

        // Collector performance metrics
        MongoCollection<Document> collectorsCol = db.getCollection("collectors");
        List<Document> collectorPerformance = new ArrayList<>();
        for (Document d : collectorsCol.find().sort(new Document("totalCollections", -1))) {
            collectorPerformance.add(d);
        }
        req.setAttribute("collectorPerformance", collectorPerformance);

        // On-time vs overdue stats from wasteRecords
        long totalFinished = db.getCollection("wasteRecords").countDocuments(
                new Document("status", new Document("$in", Arrays.asList("DISPOSED","COLLECTED"))));
        long overdueFinished = db.getCollection("wasteRecords").countDocuments(
                new Document("status", new Document("$in", Arrays.asList("DISPOSED","COLLECTED")))
                .append("overdue", true));
        long onTimeCount = totalFinished - overdueFinished;
        int onTimePct = totalFinished > 0 ? (int) Math.round((onTimeCount * 100.0) / totalFinished) : 0;
        req.setAttribute("totalFinished",   totalFinished);
        req.setAttribute("onTimeCount",     onTimeCount);
        req.setAttribute("overdueFinished", overdueFinished);
        req.setAttribute("onTimePct",       onTimePct);

        // Average response time (reported → accepted) in minutes from collectionHistory
        // We calculate from wasteRecords where both timestamps exist
        long responseSum = 0; long responseCount = 0;
        for (Document d : db.getCollection("wasteRecords")
                .find(new Document("acceptedAt", new Document("$exists", true))
                        .append("reportedAt", new Document("$exists", true))).limit(200)) {
            try {
                java.time.Instant r = Instant.parse(d.getString("reportedAt"));
                java.time.Instant a = Instant.parse(d.getString("acceptedAt"));
                long mins = Duration.between(r, a).toMinutes();
                if (mins >= 0 && mins < 1440) { responseSum += mins; responseCount++; }
            } catch (Exception ignored) {}
        }
        req.setAttribute("avgResponseMins", responseCount > 0 ? responseSum / responseCount : 0);
        req.setAttribute("responseCount",   responseCount);

                req.getRequestDispatcher("/WEB-INF/views/admin/analytics.jsp").forward(req, resp);
    }
}
