package com.meditrack.servlet;

import com.meditrack.model.Collector;
import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminCollectorsServlet
 * ------------------------
 * Admin view to manage collectors: list all, update their depot/current
 * location, or remove them.
 * GET  -> list all collectors
 * POST -> action=updateLocation (collectorId, lat, lng) | action=delete (collectorId)
 */
@WebServlet("/admin/collectors")
public class AdminCollectorsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> collectorsCol = db.getCollection("collectors");

        List<Collector> collectors = new ArrayList<>();
        for (Document d : collectorsCol.find()) {
            collectors.add(Collector.fromDocument(d));
        }
        req.setAttribute("collectors", collectors);
        req.getRequestDispatcher("/WEB-INF/views/admin/collectors.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        String collectorId = req.getParameter("collectorId");
        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> collectorsCol = db.getCollection("collectors");

        if ("updateLocation".equals(action)) {
            double lat = Double.parseDouble(req.getParameter("lat"));
            double lng = Double.parseDouble(req.getParameter("lng"));
            collectorsCol.updateOne(new Document("_id", new ObjectId(collectorId)),
                    new Document("$set", new Document("currentLat", lat).append("currentLng", lng)));
        } else if ("delete".equals(action)) {
            collectorsCol.deleteOne(new Document("_id", new ObjectId(collectorId)));
        }
        resp.sendRedirect(req.getContextPath() + "/admin/collectors");
    }
}
