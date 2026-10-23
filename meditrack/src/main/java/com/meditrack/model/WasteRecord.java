package com.meditrack.model;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * WasteRecord model - backs the "wasteRecords" collection.
 *
 * STATUS LIFECYCLE (backward-compatible — old records may have PENDING/ASSIGNED/COLLECTED/DISPOSED):
 *   PENDING → ASSIGNED → ACCEPTED → EN_ROUTE → ARRIVED → COLLECTED → DISPOSED
 *   OVERDUE (set automatically when deadline passes)
 *   CANCELLED (admin only, if needed)
 *
 * New fields added in a backward-compatible way — all nullable/optional.
 * fromDocument() safely handles missing fields (returns null/0/false).
 */
public class WasteRecord {
    private String id;
    private String hospitalId;
    private String hospitalName;
    private String wasteType;
    private double quantityKg;
    private String riskLevel;       // LOW | MEDIUM | HIGH
    private String description;
    private String reportedAt;      // ISO-8601
    private String status;          // PENDING|ASSIGNED|ACCEPTED|EN_ROUTE|ARRIVED|COLLECTED|DISPOSED|OVERDUE|CANCELLED
    private double priorityScore;
    private boolean emergency;
    private String assignedCollectorId;

    // Full journey timestamps (all nullable — old records won't have them)
    private String assignedAt;
    private String acceptedAt;
    private String routeStartedAt;
    private String arrivedAt;
    private String collectedAt;
    private String disposedAt;

    // Deadline system (new — nullable for old records)
    private String deadline;        // ISO-8601 calculated at submit time
    private boolean overdue;
    private boolean reassignmentRequested;
    private String reassignmentRequestedAt;
    private String delayReason;     // recorded when overdue: Traffic|Hospital unavailable|Vehicle issue|Other

    // Collector live location (updated by browser geolocation via CollectorLocationServlet)
    private double collectorLatitude;
    private double collectorLongitude;
    private String lastLocationUpdate;

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.append("_id", new ObjectId(id));
        doc.append("hospitalId", hospitalId)
           .append("hospitalName", hospitalName)
           .append("wasteType", wasteType)
           .append("quantityKg", quantityKg)
           .append("riskLevel", riskLevel)
           .append("description", description)
           .append("reportedAt", reportedAt)
           .append("status", status)
           .append("priorityScore", priorityScore)
           .append("emergency", emergency)
           .append("assignedCollectorId", assignedCollectorId)
           .append("assignedAt", assignedAt)
           .append("acceptedAt", acceptedAt)
           .append("routeStartedAt", routeStartedAt)
           .append("arrivedAt", arrivedAt)
           .append("collectedAt", collectedAt)
           .append("disposedAt", disposedAt)
           .append("deadline", deadline)
           .append("overdue", overdue)
           .append("reassignmentRequested", reassignmentRequested)
           .append("reassignmentRequestedAt", reassignmentRequestedAt)
           .append("delayReason", delayReason)
           .append("collectorLatitude", collectorLatitude)
           .append("collectorLongitude", collectorLongitude)
           .append("lastLocationUpdate", lastLocationUpdate);
        return doc;
    }

    public static WasteRecord fromDocument(Document doc) {
        if (doc == null) return null;
        WasteRecord w = new WasteRecord();
        w.id = doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null;
        w.hospitalId = doc.getString("hospitalId");
        w.hospitalName = doc.getString("hospitalName");
        w.wasteType = doc.getString("wasteType");
        w.quantityKg = doc.get("quantityKg") != null ? ((Number) doc.get("quantityKg")).doubleValue() : 0;
        w.riskLevel = doc.getString("riskLevel");
        w.description = doc.getString("description");
        w.reportedAt = doc.getString("reportedAt");
        w.status = doc.getString("status");
        w.priorityScore = doc.get("priorityScore") != null ? ((Number) doc.get("priorityScore")).doubleValue() : 0;
        w.emergency = doc.getBoolean("emergency", false);
        w.assignedCollectorId = doc.getString("assignedCollectorId");
        w.assignedAt = doc.getString("assignedAt");
        w.acceptedAt = doc.getString("acceptedAt");
        w.routeStartedAt = doc.getString("routeStartedAt");
        w.arrivedAt = doc.getString("arrivedAt");
        w.collectedAt = doc.getString("collectedAt");
        w.disposedAt = doc.getString("disposedAt");
        w.deadline = doc.getString("deadline");
        w.overdue = doc.getBoolean("overdue", false);
        w.reassignmentRequested = doc.getBoolean("reassignmentRequested", false);
        w.reassignmentRequestedAt = doc.getString("reassignmentRequestedAt");
        w.delayReason = doc.getString("delayReason");
        w.collectorLatitude = doc.get("collectorLatitude") != null ? ((Number) doc.get("collectorLatitude")).doubleValue() : 0;
        w.collectorLongitude = doc.get("collectorLongitude") != null ? ((Number) doc.get("collectorLongitude")).doubleValue() : 0;
        w.lastLocationUpdate = doc.getString("lastLocationUpdate");
        return w;
    }

    // getters & setters
    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getHospitalId() { return hospitalId; } public void setHospitalId(String v) { hospitalId = v; }
    public String getHospitalName() { return hospitalName; } public void setHospitalName(String v) { hospitalName = v; }
    public String getWasteType() { return wasteType; } public void setWasteType(String v) { wasteType = v; }
    public double getQuantityKg() { return quantityKg; } public void setQuantityKg(double v) { quantityKg = v; }
    public String getRiskLevel() { return riskLevel; } public void setRiskLevel(String v) { riskLevel = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getReportedAt() { return reportedAt; } public void setReportedAt(String v) { reportedAt = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public double getPriorityScore() { return priorityScore; } public void setPriorityScore(double v) { priorityScore = v; }
    public boolean isEmergency() { return emergency; } public void setEmergency(boolean v) { emergency = v; }
    public String getAssignedCollectorId() { return assignedCollectorId; } public void setAssignedCollectorId(String v) { assignedCollectorId = v; }
    public String getAssignedAt() { return assignedAt; } public void setAssignedAt(String v) { assignedAt = v; }
    public String getAcceptedAt() { return acceptedAt; } public void setAcceptedAt(String v) { acceptedAt = v; }
    public String getRouteStartedAt() { return routeStartedAt; } public void setRouteStartedAt(String v) { routeStartedAt = v; }
    public String getArrivedAt() { return arrivedAt; } public void setArrivedAt(String v) { arrivedAt = v; }
    public String getCollectedAt() { return collectedAt; } public void setCollectedAt(String v) { collectedAt = v; }
    public String getDisposedAt() { return disposedAt; } public void setDisposedAt(String v) { disposedAt = v; }
    public String getDeadline() { return deadline; } public void setDeadline(String v) { deadline = v; }
    public boolean isOverdue() { return overdue; } public void setOverdue(boolean v) { overdue = v; }
    public String getDelayReason() { return delayReason; } public void setDelayReason(String v) { delayReason = v; }
    public double getCollectorLatitude() { return collectorLatitude; } public void setCollectorLatitude(double v) { collectorLatitude = v; }
    public double getCollectorLongitude() { return collectorLongitude; } public void setCollectorLongitude(double v) { collectorLongitude = v; }
    public String getLastLocationUpdate() { return lastLocationUpdate; } public void setLastLocationUpdate(String v) { lastLocationUpdate = v; }

    public boolean isReassignmentRequested(){return reassignmentRequested;}public void setReassignmentRequested(boolean v){reassignmentRequested=v;}
    public String getReassignmentRequestedAt(){return reassignmentRequestedAt;}public void setReassignmentRequestedAt(String v){reassignmentRequestedAt=v;}
}
