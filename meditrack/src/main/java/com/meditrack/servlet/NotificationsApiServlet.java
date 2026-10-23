package com.meditrack.servlet;

import com.google.gson.Gson;
import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotificationsApiServlet
 * -------------------------
 * JSON API for the notification bell dropdown, polled by the browser every 20s.
 *
 * GET /api/notifications           — returns unread count + last 10 notifications
 * POST /api/notifications?markRead=true — marks all as read for this user
 */
@WebServlet("/api/notifications")
public class NotificationsApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.getWriter().print("{\"count\":0,\"notifications\":[]}"); return;
        }

        String userId = (String) session.getAttribute("userId");
        MongoDatabase db = MongoDBConnection.getDatabase();

        List<Map<String, Object>> items = new ArrayList<>();
        long unreadCount = 0;

        for (Document n : db.getCollection("notifications")
                .find(new Document("recipientUserId", userId))
                .sort(new Document("createdAt", -1)).limit(15)) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getObjectId("_id").toHexString());
            item.put("type", n.getString("type"));
            item.put("message", n.getString("message"));
            item.put("relatedWasteId", n.getString("relatedWasteId"));
            item.put("read", n.getBoolean("read", false));
            item.put("createdAt", n.getString("createdAt"));
            items.add(item);
            if (!n.getBoolean("read", false)) unreadCount++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", unreadCount);
        result.put("notifications", items);
        new Gson().toJson(result, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null) { resp.sendError(403); return; }
        String userId = (String) session.getAttribute("userId");
        if (userId == null) { resp.sendError(403); return; }

        MongoDatabase db = MongoDBConnection.getDatabase();
        db.getCollection("notifications").updateMany(
                new Document("recipientUserId", userId).append("read", false),
                new Document("$set", new Document("read", true)));

        resp.setContentType("application/json");
        resp.getWriter().print("{\"success\":true}");
    }
}
