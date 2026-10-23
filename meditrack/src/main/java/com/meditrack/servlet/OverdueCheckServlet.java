package com.meditrack.servlet;

import com.meditrack.util.DeadlineConfig;
import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.NotificationUtil;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * OverdueCheckServlet
 * --------------------
 * Checks all active waste records with a deadline and marks them OVERDUE
 * when the deadline has passed. Sends notifications to hospital, collector,
 * and admin. Called by the browser via AJAX every 60 seconds.
 *
 * GET /api/overdueCheck — runs the check, returns count of newly-marked-overdue
 *
 * This approach avoids needing a background thread or scheduler, keeping the
 * implementation simple and compatible with the existing Servlet architecture.
 * The downside is that records only become OVERDUE when someone has the app
 * open. For production, a proper scheduler (e.g. Quartz) would be better.
 */
@WebServlet("/api/overdueCheck")
public class OverdueCheckServlet extends HttpServlet {

    private static final List<String> ACTIVE_STATUSES = Arrays.asList(
            "PENDING", "ASSIGNED", "ACCEPTED", "EN_ROUTE", "ARRIVED");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        int marked = runCheck(MongoDBConnection.getDatabase());
        resp.getWriter().print("{\"markedOverdue\":" + marked + "}");
    }

    public static int runCheck(MongoDatabase db) {
        int count = 0;
        Document filter = new Document("status", new Document("$in", ACTIVE_STATUSES))
                .append("deadline", new Document("$exists", true).append("$ne", null))
                .append("overdue", new Document("$ne", true));

        for (Document w : db.getCollection("wasteRecords").find(filter)) {
            String deadline = w.getString("deadline");
            if (deadline == null || !DeadlineConfig.isOverdue(deadline)) continue;

            String wasteId = w.getObjectId("_id").toHexString();
            db.getCollection("wasteRecords").updateOne(
                    new Document("_id", w.getObjectId("_id")),
                    new Document("$set", new Document("status", "OVERDUE").append("overdue", true)));

            String wasteType    = w.getString("wasteType");
            String hospitalName = w.getString("hospitalName");
            String hospitalId   = w.getString("hospitalId");
            String collectorId  = w.getString("assignedCollectorId");
            String msg = "🔴 OVERDUE: " + wasteType + " collection at " + hospitalName +
                         " has passed its deadline and requires immediate attention.";

            // Notify hospital staff
            String staffId = CollectorStatusUpdateServlet.findStaffUserId(db, hospitalId);
            if (staffId != null) NotificationUtil.send(db, staffId, "OVERDUE", msg, wasteId);

            // Notify assigned collector
            if (collectorId != null) {
                Document collector = db.getCollection("collectors")
                        .find(new Document("_id", new ObjectId(collectorId))).first();
                if (collector != null) {
                    String cUserId = collector.getString("userId");
                    if (cUserId != null) NotificationUtil.send(db, cUserId, "OVERDUE",
                            "🔴 OVERDUE collection: " + wasteType + " at " + hospitalName + " — please respond immediately.", wasteId);
                }
            }

            // Notify all admins
            for (Document admin : db.getCollection("users").find(new Document("role", "ADMIN"))) {
                NotificationUtil.send(db, admin.getObjectId("_id").toHexString(), "OVERDUE", msg, wasteId);
            }

            count++;
        }
        return count;
    }
}
