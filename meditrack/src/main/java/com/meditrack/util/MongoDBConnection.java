package com.meditrack.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * MongoDBConnection
 * ------------------
 * Singleton helper that creates and reuses a single MongoClient for the
 * whole web application's lifetime. Reading a MongoClient per-request is
 * expensive (it manages an internal connection pool), so we initialize it
 * once and hand out the MongoDatabase reference to DAOs/Servlets.
 *
 * Connection string / database name can be overridden with environment
 * variables MONGO_URI and MONGO_DB, otherwise sensible local defaults are
 * used (mongodb://localhost:27017 and "meditrack").
 */
public class MongoDBConnection {

    private static volatile MongoClient client;
    private static MongoDatabase database;

    private static final String DEFAULT_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_DB = "meditrack";

    private MongoDBConnection() {
        // utility class - prevent instantiation
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            synchronized (MongoDBConnection.class) {
                if (database == null) {
                    String uri = System.getenv("MONGO_URI") != null
                            ? System.getenv("MONGO_URI") : DEFAULT_URI;
                    String dbName = System.getenv("MONGO_DB") != null
                            ? System.getenv("MONGO_DB") : DEFAULT_DB;

                    client = MongoClients.create(uri);
                    database = client.getDatabase(dbName);
                }
            }
        }
        return database;
    }

    /** Call this from a ServletContextListener on application shutdown. */
    public static void close() {
        if (client != null) {
            client.close();
            client = null;
            database = null;
        }
    }
}
