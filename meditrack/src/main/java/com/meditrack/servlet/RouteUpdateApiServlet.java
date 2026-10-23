package com.meditrack.servlet;

import com.google.gson.Gson;
import com.meditrack.util.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * RouteUpdateApiServlet
 * -----------------------
 * JSON API that returns the most recent dynamic re-optimization event for
 * a collector (old route + new route + distances), used to render the
 * "old vs updated route" side-by-side map comparison on the collector
 * dashboard without re-fetching/parsing HTML.
 *
 * GET /api/routeUpdate?collectorId=xxx
 */
@WebServlet("/api/routeUpdate")
public class RouteUpdateApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String collectorId = req.getParameter("collectorId");
        Map<String, Object> result = new HashMap<>();

        if (collectorId != null) {
            MongoDatabase db = MongoDBConnection.getDatabase();
            MongoCollection<Document> updatesCol = db.getCollection("routeUpdates");
            Document latest = updatesCol.find(new Document("collectorId", collectorId))
                    .sort(new Document("createdAt", -1)).first();

            if (latest != null) {
                result.put("oldRoute", latest.get("oldRoute"));
                result.put("newRoute", latest.get("newRoute"));
                result.put("oldDistanceKm", latest.get("oldDistanceKm"));
                result.put("newDistanceKm", latest.get("newDistanceKm"));
                result.put("reason", latest.getString("reason"));
            }
        }

        try (PrintWriter out = resp.getWriter()) {
            out.print(new Gson().toJson(result));
        }
    }
}
