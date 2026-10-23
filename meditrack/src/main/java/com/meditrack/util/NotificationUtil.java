package com.meditrack.util;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.time.Instant;

/**
 * NotificationUtil
 * -----------------
 * Writes in-app notifications to the "notifications" MongoDB collection.
 * Hospital staff see these as a banner on their dashboard the next time
 * they load it — zero extra jars, zero SMTP setup, zero new failure points.
 *
 * Notification types used in Meditrack:
 *   DISPOSAL_COMPLETE  — fired when a collector marks waste as DISPOSED
 *   ASSIGNED           — fired when waste is added to a collector's route
 *   EMERGENCY          — fired when a high-risk record triggers re-optimization
 */
public class NotificationUtil {

    public static void send(MongoDatabase db, String recipientUserId,
                            String type, String message, String relatedWasteId) {
        try {
            MongoCollection<Document> col = db.getCollection("notifications");
            col.insertOne(new Document()
                    .append("recipientUserId", recipientUserId)
                    .append("type",            type)
                    .append("message",         message)
                    .append("relatedWasteId",  relatedWasteId)
                    .append("read",            false)
                    .append("createdAt",       Instant.now().toString()));
        } catch (Exception e) {
            // Notification failure must never break the main flow
            System.err.println("[NotificationUtil] Failed to send notification: " + e.getMessage());
        }
    }

    /** Marks all unread notifications for a user as read (call on dashboard load). */
    public static void markAllRead(MongoDatabase db, String userId) {
        try {
            db.getCollection("notifications").updateMany(
                    new Document("recipientUserId", userId).append("read", false),
                    new Document("$set", new Document("read", true)));
        } catch (Exception e) {
            System.err.println("[NotificationUtil] Failed to mark notifications read: " + e.getMessage());
        }
    }
}
