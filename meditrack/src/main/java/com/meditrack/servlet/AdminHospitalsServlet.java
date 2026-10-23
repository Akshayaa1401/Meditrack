package com.meditrack.servlet;

import com.meditrack.model.Hospital;
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
 * AdminHospitalsServlet
 * -----------------------
 * Admin view to manage (list / delete) registered hospitals.
 * GET  -> list all hospitals
 * POST -> delete a hospital (action=delete&hospitalId=...)
 */
@WebServlet("/admin/hospitals")
public class AdminHospitalsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> hospitalsCol = db.getCollection("hospitals");

        List<Hospital> hospitals = new ArrayList<>();
        for (Document d : hospitalsCol.find()) {
            hospitals.add(Hospital.fromDocument(d));
        }
        req.setAttribute("hospitals", hospitals);
        req.getRequestDispatcher("/WEB-INF/views/admin/hospitals.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        String hospitalId = req.getParameter("hospitalId");

        MongoDatabase db = MongoDBConnection.getDatabase();
        if ("delete".equals(action) && hospitalId != null) {
            db.getCollection("hospitals").deleteOne(new Document("_id", new ObjectId(hospitalId)));
        }
        resp.sendRedirect(req.getContextPath() + "/admin/hospitals");
    }
}
