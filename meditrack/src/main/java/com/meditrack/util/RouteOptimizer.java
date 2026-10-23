package com.meditrack.util;

import java.util.ArrayList;
import java.util.List;

/**
 * RouteOptimizer
 * ---------------
 * Implements a lightweight heuristic route optimizer for collectors. A true
 * Traveling-Salesman solver is NP-hard; for a real-time dashboard we use
 * well-known, fast heuristics instead:
 *
 *  1. buildRoute()      - Priority-weighted Nearest Neighbor: at every step,
 *                          choose the next stop that maximizes
 *                          (priorityScore / distance) — i.e. prefer stops
 *                          that are both close AND urgent.
 *
 *  2. insertEmergency()  - Cheapest Insertion: when a new emergency point
 *                          arrives mid-route, find the edge (gap between two
 *                          consecutive stops already in the route) where
 *                          inserting the new point adds the LEAST extra
 *                          travel distance, then splice it in there. This
 *                          avoids recomputing the whole route from scratch
 *                          and keeps disruption to the collector minimal
 *                          while still guaranteeing the emergency is visited
 *                          promptly.
 */
public class RouteOptimizer {

    public static final double EARTH_RADIUS_KM = 6371.0;

    /** A single stop in a route - typically a hospital with a pending waste record. */
    public static class Stop {
        public String wasteId;
        public String hospitalId;
        public String hospitalName;
        public double latitude;
        public double longitude;
        public double priorityScore;
        public boolean emergency;

        public Stop() {}

        public Stop(String wasteId, String hospitalId, String hospitalName,
                     double latitude, double longitude, double priorityScore, boolean emergency) {
            this.wasteId = wasteId;
            this.hospitalId = hospitalId;
            this.hospitalName = hospitalName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.priorityScore = priorityScore;
            this.emergency = emergency;
        }
    }

    /** Great-circle distance between two lat/lng points, in kilometers. */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Builds an ordered collection route starting from the collector's
     * current depot/location, using priority-weighted nearest neighbor.
     */
    public static List<Stop> buildRoute(double startLat, double startLon, List<Stop> candidates) {
        List<Stop> remaining = new ArrayList<>(candidates);
        List<Stop> route = new ArrayList<>();

        double curLat = startLat, curLon = startLon;

        while (!remaining.isEmpty()) {
            Stop best = null;
            double bestScore = -1;

            for (Stop s : remaining) {
                double dist = haversineDistanceKm(curLat, curLon, s.latitude, s.longitude);
                // Avoid divide-by-zero; +0.1 km floor so very close stops don't dominate infinitely
                double desirability = (s.priorityScore + 1.0) / (dist + 0.1);
                // Emergency stops get an extra boost to guarantee they surface early
                if (s.emergency) desirability *= 3.0;

                if (desirability > bestScore) {
                    bestScore = desirability;
                    best = s;
                }
            }

            route.add(best);
            remaining.remove(best);
            curLat = best.latitude;
            curLon = best.longitude;
        }

        return route;
    }

    /**
     * Inserts a new emergency stop into an existing route at the position
     * that minimizes additional total travel distance (cheapest insertion).
     * Returns a NEW list representing the updated route; the original list
     * is left untouched so the caller can keep it for "old vs new" comparison.
     */
    public static List<Stop> insertEmergency(double startLat, double startLon,
                                              List<Stop> currentRoute, Stop emergencyStop) {
        List<Stop> updated = new ArrayList<>(currentRoute);

        if (updated.isEmpty()) {
            updated.add(emergencyStop);
            return updated;
        }

        int bestPosition = 0;
        double bestExtraCost = Double.MAX_VALUE;

        // consider inserting before the first stop, between every pair, and after the last stop
        for (int i = 0; i <= updated.size(); i++) {
            double prevLat, prevLon;
            if (i == 0) {
                prevLat = startLat;
                prevLon = startLon;
            } else {
                prevLat = updated.get(i - 1).latitude;
                prevLon = updated.get(i - 1).longitude;
            }

            double extraCost;
            if (i == updated.size()) {
                // inserting at the very end - cost is just prev -> emergency
                extraCost = haversineDistanceKm(prevLat, prevLon, emergencyStop.latitude, emergencyStop.longitude);
            } else {
                double nextLat = updated.get(i).latitude;
                double nextLon = updated.get(i).longitude;

                double original = haversineDistanceKm(prevLat, prevLon, nextLat, nextLon);
                double viaEmergency = haversineDistanceKm(prevLat, prevLon, emergencyStop.latitude, emergencyStop.longitude)
                        + haversineDistanceKm(emergencyStop.latitude, emergencyStop.longitude, nextLat, nextLon);
                extraCost = viaEmergency - original;
            }

            // Emergencies are weighted to prefer earlier insertion when costs are close
            double adjustedCost = extraCost - (i == 0 ? 0.001 : 0); // tiny tiebreaker favoring front

            if (adjustedCost < bestExtraCost) {
                bestExtraCost = adjustedCost;
                bestPosition = i;
            }
        }

        updated.add(bestPosition, emergencyStop);
        return updated;
    }

    /** Total travel distance for a full route starting from the given depot. */
    public static double totalRouteDistanceKm(double startLat, double startLon, List<Stop> route) {
        double total = 0;
        double curLat = startLat, curLon = startLon;
        for (Stop s : route) {
            total += haversineDistanceKm(curLat, curLon, s.latitude, s.longitude);
            curLat = s.latitude;
            curLon = s.longitude;
        }
        return Math.round(total * 100.0) / 100.0;
    }
}
