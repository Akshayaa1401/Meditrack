package com.meditrack.model;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.ArrayList;

/**
 * CollectionAssignment model - backs the "collectionAssignments" collection.
 * Represents an active route assigned to a collector: an ordered list of
 * waste-record stop IDs plus geo-coordinates, used for map rendering and
 * for dynamic re-optimization when emergencies arrive.
 */
public class CollectionAssignment {
    private String id;
    private String collectorId;
    private List<Document> route;     // ordered list of stop documents (wasteId, hospitalId, lat, lng, priorityScore, emergency)
    private double totalDistanceKm;
    private String status;            // ACTIVE | COMPLETED
    private String createdAt;
    private String updatedAt;

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.append("_id", new ObjectId(id));
        doc.append("collectorId", collectorId)
           .append("route", route != null ? route : new ArrayList<Document>())
           .append("totalDistanceKm", totalDistanceKm)
           .append("status", status)
           .append("createdAt", createdAt)
           .append("updatedAt", updatedAt);
        return doc;
    }

    @SuppressWarnings("unchecked")
    public static CollectionAssignment fromDocument(Document doc) {
        if (doc == null) return null;
        CollectionAssignment a = new CollectionAssignment();
        a.setId(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null);
        a.setCollectorId(doc.getString("collectorId"));
        a.setRoute((List<Document>) doc.get("route"));
        a.setTotalDistanceKm(doc.get("totalDistanceKm") != null ? ((Number) doc.get("totalDistanceKm")).doubleValue() : 0);
        a.setStatus(doc.getString("status"));
        a.setCreatedAt(doc.getString("createdAt"));
        a.setUpdatedAt(doc.getString("updatedAt"));
        return a;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }
    public List<Document> getRoute() { return route; }
    public void setRoute(List<Document> route) { this.route = route; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
