package com.meditrack.servlet;

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
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * LoginServlet
 * -------------
 * Authenticates a user and starts a session. On success, redirects to the
 * role-appropriate dashboard. Session attributes set: userId, fullName,
 * role, hospitalId (if applicable), collectorId (if applicable).
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        Document doc = users.find(new Document("email", email)).first();

        if (doc == null || !PasswordUtil.verifyPassword(password, doc.getString("passwordHash"))) {
            req.setAttribute("error", "Invalid email or password.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        User user = User.fromDocument(doc);

        HttpSession session = req.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("fullName", user.getFullName());
        session.setAttribute("role", user.getRole());
        session.setAttribute("hospitalId", user.getHospitalId());
        session.setAttribute("collectorId", user.getCollectorId());
        session.setMaxInactiveInterval(30 * 60); // 30 minutes

        String ctx = req.getContextPath();
        switch (user.getRole()) {
            case "ADMIN":
                resp.sendRedirect(ctx + "/admin/dashboard");
                break;
            case "HOSPITAL_STAFF":
                resp.sendRedirect(ctx + "/hospital/dashboard");
                break;
            case "COLLECTOR":
                resp.sendRedirect(ctx + "/collector/dashboard");
                break;
            default:
                resp.sendRedirect(ctx + "/login.jsp");
        }
    }
}
