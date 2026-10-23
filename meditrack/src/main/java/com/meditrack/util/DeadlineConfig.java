package com.meditrack.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * DeadlineConfig
 * ---------------
 * Configurable deadline hours for each risk/emergency level.
 * Change these constants to adjust deadlines without touching business logic.
 *
 * IMPORTANT: These are operational deadlines for the system only.
 * They are NOT based on any specific medical or legal regulation.
 * A real deployment should verify these against applicable regulations
 * and configure them accordingly.
 *
 * To temporarily shorten deadlines for testing, set the system property:
 *   -DMEDITRACK_TEST_MODE=true
 * This reduces all deadlines to 2 minutes for easy testing.
 */
public class DeadlineConfig {

    // --- CONFIGURABLE DEADLINE HOURS ---
    public static final int EMERGENCY_HOURS = 2;   // Emergency: 2 hours
    public static final int HIGH_HOURS      = 4;   // High risk: 4 hours
    public static final int MEDIUM_HOURS    = 8;   // Medium risk: 8 hours
    public static final int LOW_HOURS       = 24;  // Low risk: 24 hours

    // Warning threshold: notify when this % of deadline time has passed
    public static final double APPROACHING_THRESHOLD = 0.75;

    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getProperty("MEDITRACK_TEST_MODE"));
    private static final int TEST_MINUTES = 2; // shorten to 2 minutes in test mode

    /**
     * Returns the deadline as an ISO-8601 string, calculated from now.
     */
    public static String calculateDeadline(String riskLevel, boolean emergency) {
        if (TEST_MODE) {
            return Instant.now().plus(TEST_MINUTES, ChronoUnit.MINUTES).toString();
        }
        int hours = getDeadlineHours(riskLevel, emergency);
        return Instant.now().plus(hours, ChronoUnit.HOURS).toString();
    }

    public static int getDeadlineHours(String riskLevel, boolean emergency) {
        if (emergency) return EMERGENCY_HOURS;
        if ("HIGH".equalsIgnoreCase(riskLevel)) return HIGH_HOURS;
        if ("MEDIUM".equalsIgnoreCase(riskLevel)) return MEDIUM_HOURS;
        return LOW_HOURS;
    }

    /**
     * Returns true if the deadline has passed.
     */
    public static boolean isOverdue(String deadlineIso) {
        if (deadlineIso == null) return false;
        try {
            return Instant.parse(deadlineIso).isBefore(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if >75% of the deadline window has elapsed (approaching).
     */
    public static boolean isApproaching(String reportedAtIso, String deadlineIso) {
        if (reportedAtIso == null || deadlineIso == null) return false;
        try {
            Instant reported = Instant.parse(reportedAtIso);
            Instant deadline = Instant.parse(deadlineIso);
            Instant now = Instant.now();
            long total = deadline.toEpochMilli() - reported.toEpochMilli();
            long elapsed = now.toEpochMilli() - reported.toEpochMilli();
            if (total <= 0) return false;
            return ((double) elapsed / total) >= APPROACHING_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns a human-friendly "Xh Ym remaining" or "X minutes remaining" string.
     * Returns "OVERDUE" if past deadline.
     */
    public static String remainingTimeLabel(String deadlineIso) {
        if (deadlineIso == null) return "N/A";
        try {
            Instant deadline = Instant.parse(deadlineIso);
            Instant now = Instant.now();
            if (now.isAfter(deadline)) return "OVERDUE";
            long totalMinutes = ChronoUnit.MINUTES.between(now, deadline);
            if (totalMinutes >= 60) {
                long hours = totalMinutes / 60;
                long mins = totalMinutes % 60;
                return hours + "h " + mins + "m remaining";
            }
            return totalMinutes + " min remaining";
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Returns CSS class for deadline status: "deadline-ok", "deadline-approaching", "deadline-overdue"
     */
    public static String deadlineStatusClass(String reportedAtIso, String deadlineIso) {
        if (isOverdue(deadlineIso)) return "deadline-overdue";
        if (isApproaching(reportedAtIso, deadlineIso)) return "deadline-approaching";
        return "deadline-ok";
    }
}
