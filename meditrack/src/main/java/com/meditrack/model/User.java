package com.meditrack.model;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * User model - backs the "users" collection.
 * Roles: ADMIN, HOSPITAL_STAFF, COLLECTOR
 */
public class User {
    private String id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String role;          // ADMIN | HOSPITAL_STAFF | COLLECTOR
    private String hospitalId;    // set if role == HOSPITAL_STAFF
    private String collectorId;   // set if role == COLLECTOR
    private String phone;
    private String createdAt;

    public User() {}

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.append("_id", new ObjectId(id));
        doc.append("fullName", fullName)
           .append("email", email)
           .append("passwordHash", passwordHash)
           .append("role", role)
           .append("hospitalId", hospitalId)
           .append("collectorId", collectorId)
           .append("phone", phone)
           .append("createdAt", createdAt);
        return doc;
    }

    public static User fromDocument(Document doc) {
        if (doc == null) return null;
        User u = new User();
        u.setId(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null);
        u.setFullName(doc.getString("fullName"));
        u.setEmail(doc.getString("email"));
        u.setPasswordHash(doc.getString("passwordHash"));
        u.setRole(doc.getString("role"));
        u.setHospitalId(doc.getString("hospitalId"));
        u.setCollectorId(doc.getString("collectorId"));
        u.setPhone(doc.getString("phone"));
        u.setCreatedAt(doc.getString("createdAt"));
        return u;
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
