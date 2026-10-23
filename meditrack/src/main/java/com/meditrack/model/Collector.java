package com.meditrack.model;

import org.bson.Document;
import org.bson.types.ObjectId;

/** Collector model - backs the "collectors" collection. */
public class Collector {
    private String id;
    private String userId;
    private String fullName;
    private String vehicleNumber;
    private double currentLat;
    private double currentLng;
    private String status;       // AVAILABLE | ON_ROUTE | OFF_DUTY
    private int totalCollections;
    private String createdAt;

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.append("_id", new ObjectId(id));
        doc.append("userId", userId)
           .append("fullName", fullName)
           .append("vehicleNumber", vehicleNumber)
           .append("currentLat", currentLat)
           .append("currentLng", currentLng)
           .append("status", status)
           .append("totalCollections", totalCollections)
           .append("createdAt", createdAt);
        return doc;
    }

    public static Collector fromDocument(Document doc) {
        if (doc == null) return null;
        Collector c = new Collector();
        c.setId(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null);
        c.setUserId(doc.getString("userId"));
        c.setFullName(doc.getString("fullName"));
        c.setVehicleNumber(doc.getString("vehicleNumber"));
        c.setCurrentLat(doc.get("currentLat") != null ? ((Number) doc.get("currentLat")).doubleValue() : 0);
        c.setCurrentLng(doc.get("currentLng") != null ? ((Number) doc.get("currentLng")).doubleValue() : 0);
        c.setStatus(doc.getString("status"));
        c.setTotalCollections(doc.getInteger("totalCollections", 0));
        c.setCreatedAt(doc.getString("createdAt"));
        return c;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public double getCurrentLat() { return currentLat; }
    public void setCurrentLat(double currentLat) { this.currentLat = currentLat; }
    public double getCurrentLng() { return currentLng; }
    public void setCurrentLng(double currentLng) { this.currentLng = currentLng; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalCollections() { return totalCollections; }
    public void setTotalCollections(int totalCollections) { this.totalCollections = totalCollections; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
