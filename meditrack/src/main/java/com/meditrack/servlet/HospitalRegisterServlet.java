package com.meditrack.servlet;

import com.meditrack.model.Hospital;
import com.meditrack.util.MongoDBConnection;
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
import java.time.Instant;

/**
 * HospitalRegisterServlet
 * ------------------------
 * Lets logged-in hospital staff register their hospital's profile, address,
 * and map coordinates (latitude/longitude captured via the Leaflet click-to-
 * pick-location widget on the JSP page). Also links the hospital back to the
 * staff user's session/account.
 */
@WebServlet("/hospital/register")
public class HospitalRegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/hospital/registerHospital.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String address = req.getParameter("address");
        String latStr = req.getParameter("latitude");
        String lngStr = req.getParameter("longitude");
        String contactPerson = req.getParameter("contactPerson");
        String contactPhone = req.getParameter("contactPhone");

        if (name == null || name.trim().isEmpty() || latStr == null || lngStr == null) {
            req.setAttribute("error", "Hospital name and map location are required.");
            req.getRequestDispatcher("/WEB-INF/views/hospital/registerHospital.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession();
        String userId = (String) session.getAttribute("userId");

        Hospital hospital = new Hospital();
        hospital.setName(name);
        hospital.setAddress(address);
        hospital.setLatitude(Double.parseDouble(latStr));
        hospital.setLongitude(Double.parseDouble(lngStr));
        hospital.setContactPerson(contactPerson);
        hospital.setContactPhone(contactPhone);
        hospital.setRegisteredBy(userId);
        hospital.setCreatedAt(Instant.now().toString());

        MongoDatabase db = MongoDBConnection.getDatabase();
        MongoCollection<Document> hospitals = db.getCollection("hospitals");
        Document doc = hospital.toDocument();
        doc.append("registeredByUserId", userId);
        hospitals.insertOne(doc);
        String hospitalId = doc.getObjectId("_id").toHexString();

        // Link this hospital to the logged-in user's account
        MongoCollection<Document> users = db.getCollection("users");
        users.updateOne(new Document("_id", new org.bson.types.ObjectId(userId)),
                new Document("$set", new Document("hospitalId", hospitalId)));
        session.setAttribute("hospitalId", hospitalId);

        resp.sendRedirect(req.getContextPath() + "/hospital/dashboard?registered=true");
    }
}
