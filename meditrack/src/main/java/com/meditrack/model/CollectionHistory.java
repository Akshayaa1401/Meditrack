package com.meditrack.model;

import org.bson.Document;
import org.bson.types.ObjectId;

/** CollectionHistory model - backs the "collectionHistory" collection (audit trail of completed collections). */
public class CollectionHistory {
    private String id;
    private String wasteId;
    private String hospitalId;
    private String hospitalName;
    private String collectorId;
    private String wasteType;
    private double quantityKg;
    private String riskLevel;
    private String collectedAt;
    private String disposedAt;
    private double priorityScoreAtCollection;

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.append("_id", new ObjectId(id));
        doc.append("wasteId", wasteId)
           .append("hospitalId", hospitalId)
           .append("hospitalName", hospitalName)
           .append("collectorId", collectorId)
           .append("wasteType", wasteType)
           .append("quantityKg", quantityKg)
           .append("riskLevel", riskLevel)
           .append("collectedAt", collectedAt)
           .append("disposedAt", disposedAt)
           .append("priorityScoreAtCollection", priorityScoreAtCollection);
        return doc;
    }

    public static CollectionHistory fromDocument(Document doc) {
        if (doc == null) return null;
        CollectionHistory h = new CollectionHistory();
        h.setId(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null);
        h.setWasteId(doc.getString("wasteId"));
        h.setHospitalId(doc.getString("hospitalId"));
        h.setHospitalName(doc.getString("hospitalName"));
        h.setCollectorId(doc.getString("collectorId"));
        h.setWasteType(doc.getString("wasteType"));
        h.setQuantityKg(doc.get("quantityKg") != null ? ((Number) doc.get("quantityKg")).doubleValue() : 0);
        h.setRiskLevel(doc.getString("riskLevel"));
        h.setCollectedAt(doc.getString("collectedAt"));
        h.setDisposedAt(doc.getString("disposedAt"));
        h.setPriorityScoreAtCollection(doc.get("priorityScoreAtCollection") != null ? ((Number) doc.get("priorityScoreAtCollection")).doubleValue() : 0);
        return h;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWasteId() { return wasteId; }
    public void setWasteId(String wasteId) { this.wasteId = wasteId; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }
    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }
    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getCollectedAt() { return collectedAt; }
    public void setCollectedAt(String collectedAt) { this.collectedAt = collectedAt; }
    public String getDisposedAt() { return disposedAt; }
    public void setDisposedAt(String disposedAt) { this.disposedAt = disposedAt; }
    public double getPriorityScoreAtCollection() { return priorityScoreAtCollection; }
    public void setPriorityScoreAtCollection(double priorityScoreAtCollection) { this.priorityScoreAtCollection = priorityScoreAtCollection; }
}
