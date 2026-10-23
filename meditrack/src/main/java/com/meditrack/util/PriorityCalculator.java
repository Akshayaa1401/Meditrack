package com.meditrack.util;

import java.time.Duration;
import java.time.Instant;

/**
 * PriorityCalculator
 * -------------------
 * Computes a single numeric "priority score" for a waste record so that
 * collection requests can be ranked. The score blends three signals:
 *
 *   1. Risk Level   (40% weight) - High risk biomedical waste (e.g. sharps,
 *                                  infectious, pathological) must jump the
 *                                  queue ahead of low-risk general waste.
 *   2. Quantity     (30% weight) - Larger quantities create more exposure
 *                                  risk and storage burden at the hospital.
 *   3. Aging factor (30% weight) - The longer a request waits uncollected,
 *                                  the more urgent it becomes (prevents
 *                                  starvation of lower-risk requests).
 *
 * Final score range is roughly 0-100+ (aging is unbounded so very old
 * requests can still surface even if low risk/quantity).
 */
public class PriorityCalculator {

    private static final double RISK_WEIGHT = 0.40;
    private static final double QUANTITY_WEIGHT = 0.30;
    private static final double AGING_WEIGHT = 0.30;

    // Quantity (kg) considered "maximal" for normalization purposes
    private static final double QUANTITY_NORMALIZATION_CAP = 100.0;

    // After this many hours of waiting, aging factor saturates at 100
    private static final double AGING_SATURATION_HOURS = 48.0;

    /** Emergency threshold: scores >= this value are treated as emergencies */
    public static final double EMERGENCY_THRESHOLD = 75.0;

    public static double riskScore(String riskLevel) {
        if (riskLevel == null) return 0;
        switch (riskLevel.toUpperCase()) {
            case "HIGH":   return 100.0;
            case "MEDIUM": return 60.0;
            case "LOW":    return 25.0;
            default:       return 0.0;
        }
    }

    public static double quantityScore(double quantityKg) {
        double normalized = Math.min(quantityKg, QUANTITY_NORMALIZATION_CAP) / QUANTITY_NORMALIZATION_CAP;
        return normalized * 100.0;
    }

    public static double agingScore(Instant reportedAt) {
        if (reportedAt == null) return 0;
        long hoursWaiting = Duration.between(reportedAt, Instant.now()).toHours();
        double normalized = Math.min(hoursWaiting, AGING_SATURATION_HOURS) / AGING_SATURATION_HOURS;
        return normalized * 100.0;
    }

    /**
     * Calculates the overall weighted priority score for a waste record.
     */
    public static double calculatePriority(String riskLevel, double quantityKg, Instant reportedAt) {
        double risk = riskScore(riskLevel) * RISK_WEIGHT;
        double qty = quantityScore(quantityKg) * QUANTITY_WEIGHT;
        double aging = agingScore(reportedAt) * AGING_WEIGHT;
        double total = risk + qty + aging;
        return Math.round(total * 100.0) / 100.0; // 2 decimal places
    }

    public static boolean isEmergency(String riskLevel, double quantityKg, Instant reportedAt) {
        // An emergency is explicitly any HIGH risk waste regardless of score,
        // OR any record whose computed score crosses the emergency threshold.
        if ("HIGH".equalsIgnoreCase(riskLevel)) return true;
        return calculatePriority(riskLevel, quantityKg, reportedAt) >= EMERGENCY_THRESHOLD;
    }
}
