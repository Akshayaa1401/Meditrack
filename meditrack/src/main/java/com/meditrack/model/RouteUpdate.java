package com.meditrack.model;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.List;

/**
 * RouteUpdate model - backs the "routeUpdates" collection.
 * Stores a record every time dynamic re-optimization happens, capturing
 * the OLD route and the NEW route so the UI can show a before/after
 * comparison (the "Dynamic Route Re-Optimization" feature).
 */
public class RouteUpdate {
    private String id;
    private String collectorId;
    private String triggeringWasteId;   // the new emergency waste record that caused the change
    private String reason;
    private List<Document> oldRoute;
    private List<Document> newRoute;
    private double oldDistanceKm;
    private double newDistanceKm;
    private String createdAt;

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.append("_id", new ObjectId(id));
        doc.append("collectorId", collectorId)
           .append("triggeringWasteId", triggeringWasteId)
           .append("reason", reason)
           .append("oldRoute", oldRoute)
           .append("newRoute", newRoute)
           .append("oldDistanceKm", oldDistanceKm)
           .append("newDistanceKm", newDistanceKm)
           .append("createdAt", createdAt);
        return doc;
    }

    @SuppressWarnings("unchecked")
    public static RouteUpdate fromDocument(Document doc) {
        if (doc == null) return null;
        RouteUpdate r = new RouteUpdate();
        r.setId(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null);
        r.setCollectorId(doc.getString("collectorId"));
        r.setTriggeringWasteId(doc.getString("triggeringWasteId"));
        r.setReason(doc.getString("reason"));
        r.setOldRoute((List<Document>) doc.get("oldRoute"));
        r.setNewRoute((List<Document>) doc.get("newRoute"));
        r.setOldDistanceKm(doc.get("oldDistanceKm") != null ? ((Number) doc.get("oldDistanceKm")).doubleValue() : 0);
        r.setNewDistanceKm(doc.get("newDistanceKm") != null ? ((Number) doc.get("newDistanceKm")).doubleValue() : 0);
        r.setCreatedAt(doc.getString("createdAt"));
        return r;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }
    public String getTriggeringWasteId() { return triggeringWasteId; }
    public void setTriggeringWasteId(String triggeringWasteId) { this.triggeringWasteId = triggeringWasteId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<Document> getOldRoute() { return oldRoute; }
    public void setOldRoute(List<Document> oldRoute) { this.oldRoute = oldRoute; }
    public List<Document> getNewRoute() { return newRoute; }
    public void setNewRoute(List<Document> newRoute) { this.newRoute = newRoute; }
    public double getOldDistanceKm() { return oldDistanceKm; }
    public void setOldDistanceKm(double oldDistanceKm) { this.oldDistanceKm = oldDistanceKm; }
    public double getNewDistanceKm() { return newDistanceKm; }
    public void setNewDistanceKm(double newDistanceKm) { this.newDistanceKm = newDistanceKm; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
