package com.meditrack.servlet;

import com.meditrack.model.Collector;
import com.meditrack.model.User;
import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.PasswordUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

/**
 * RegisterServlet
 * ----------------
 * Handles new account registration for all three roles. Hospital staff and
 * Collectors get a minimal linked record auto-created (hospital details are
 * filled in later via HospitalRegisterServlet; collector profile is created
 * immediately with status AVAILABLE).
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String role = req.getParameter("role"); // ADMIN | HOSPITAL_STAFF | COLLECTOR
        String phone = req.getParameter("phone");

        if (fullName == null || email == null || password == null || role == null
                || fullName.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            req.setAttribute("error", "All fields are required.");
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
            return;
        }

        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> users = db.getCollection("users");

        if (users.find(new Document("email", email)).first() != null) {
            req.setAttribute("error", "An account with this email already exists.");
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
            return;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setRole(role);
        user.setPhone(phone);
        user.setCreatedAt(Instant.now().toString());

        Document userDoc = user.toDocument();
        users.insertOne(userDoc);
        String newUserId = userDoc.getObjectId("_id").toHexString();

        // If registering as a collector, auto-create a linked collector profile
        if ("COLLECTOR".equals(role)) {
            MongoCollection<Document> collectors = db.getCollection("collectors");
            Collector collector = new Collector();
            collector.setUserId(newUserId);
            collector.setFullName(fullName);
            collector.setVehicleNumber(req.getParameter("vehicleNumber"));
            collector.setStatus("AVAILABLE");
            collector.setTotalCollections(0);
            collector.setCreatedAt(Instant.now().toString());
            Document collectorDoc = collector.toDocument();
            collectors.insertOne(collectorDoc);

            users.updateOne(new Document("_id", userDoc.getObjectId("_id")),
                    new Document("$set", new Document("collectorId", collectorDoc.getObjectId("_id").toHexString())));
        }

        resp.sendRedirect(req.getContextPath() + "/login.jsp?registered=true");
    }
}
