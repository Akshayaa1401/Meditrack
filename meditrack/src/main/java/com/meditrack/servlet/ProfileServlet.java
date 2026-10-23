package com.meditrack.servlet;

import com.meditrack.util.MongoDBConnection;
import com.meditrack.util.PasswordUtil;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * ProfileServlet — view profile + change password.
 * GET  /profile  → show profile page
 * POST /profile  → update password
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp"); return;
        }
        String userId = (String) session.getAttribute("userId");
        MongoDatabase db = MongoDBConnection.getDatabase();
        Document user = db.getCollection("users")
                .find(new Document("_id", new ObjectId(userId))).first();
        req.setAttribute("user", user);

        // Collector extra info
        String collectorId = (String) session.getAttribute("collectorId");
        if (collectorId != null) {
            Document collector = db.getCollection("collectors")
                    .find(new Document("_id", new ObjectId(collectorId))).first();
            req.setAttribute("collector", collector);
        }
        // Hospital extra info
        String hospitalId = (String) session.getAttribute("hospitalId");
        if (hospitalId != null) {
            Document hospital = db.getCollection("hospitals")
                    .find(new Document("_id", new ObjectId(hospitalId))).first();
            req.setAttribute("hospital", hospital);
        }
        req.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp"); return;
        }
        String userId      = (String) session.getAttribute("userId");
        String currentPw   = req.getParameter("currentPassword");
        String newPw       = req.getParameter("newPassword");
        String confirmPw   = req.getParameter("confirmPassword");

        MongoDatabase db = MongoDBConnection.getDatabase();
        Document user = db.getCollection("users")
                .find(new Document("_id", new ObjectId(userId))).first();

        if (user == null) { resp.sendRedirect(req.getContextPath() + "/profile?error=notfound"); return; }

        if (!PasswordUtil.verifyPassword(currentPw, user.getString("passwordHash"))) {
            resp.sendRedirect(req.getContextPath() + "/profile?error=wrongpassword"); return;
        }
        if (newPw == null || newPw.length() < 6) {
            resp.sendRedirect(req.getContextPath() + "/profile?error=tooshort"); return;
        }
        if (!newPw.equals(confirmPw)) {
            resp.sendRedirect(req.getContextPath() + "/profile?error=mismatch"); return;
        }

        db.getCollection("users").updateOne(
                new Document("_id", new ObjectId(userId)),
                new Document("$set", new Document("passwordHash",
                        PasswordUtil.hashPassword(newPw))));

        resp.sendRedirect(req.getContextPath() + "/profile?success=true");
    }
}
