package com.meditrack.model;

import org.bson.Document;
import org.bson.types.ObjectId;

/** Hospital model - backs the "hospitals" collection. */
public class Hospital {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String contactPerson;
    private String contactPhone;
    private String registeredBy;   // userId of hospital staff who registered it
    private String createdAt;

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.append("_id", new ObjectId(id));
        doc.append("name", name)
           .append("address", address)
           .append("latitude", latitude)
           .append("longitude", longitude)
           .append("contactPerson", contactPerson)
           .append("contactPhone", contactPhone)
           .append("registeredBy", registeredBy)
           .append("createdAt", createdAt);
        return doc;
    }

    public static Hospital fromDocument(Document doc) {
        if (doc == null) return null;
        Hospital h = new Hospital();
        h.setId(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null);
        h.setName(doc.getString("name"));
        h.setAddress(doc.getString("address"));
        h.setLatitude(doc.get("latitude") != null ? ((Number) doc.get("latitude")).doubleValue() : 0);
        h.setLongitude(doc.get("longitude") != null ? ((Number) doc.get("longitude")).doubleValue() : 0);
        h.setContactPerson(doc.getString("contactPerson"));
        h.setContactPhone(doc.getString("contactPhone"));
        h.setRegisteredBy(doc.getString("registeredBy"));
        h.setCreatedAt(doc.getString("createdAt"));
        return h;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
// Note: registeredByUserId field is stored directly in MongoDB by HospitalRegisterServlet
// but not mapped here since Hospital model is used read-only for display.
// The servlet reads/writes this field via raw Document operations.

// Separate class to hold registeredByUserId without breaking the existing model
// Actually: we'll patch the toDocument to include it directly
