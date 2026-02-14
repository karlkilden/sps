package com.kildeen.sps.persistence;

import com.kildeen.sps.ConfigurationException;

/**
 * Provides access to the configured database instance.
 * Must be configured before use via {@link #configure(Database)}.
 */
public class DatabaseProvider {

    private static volatile Database DATABASE;

    /**
     * Configures the database instance to use.
     *
     * @param database the database implementation
     * @return the configured database
     */
    public static Database configure(Database database) {
        DATABASE = database;
        return DATABASE;
    }

    /**
     * Returns the configured database, or null if not configured.
     * Prefer {@link #require()} for most use cases.
     *
     * @return the database or null
     */
    public static Database database() {
        return DATABASE;
    }

    /**
     * Returns the configured database, throwing if not configured.
     * Use this method when database is required.
     *
     * @return the database
     * @throws ConfigurationException if database not configured
     */
    public static Database require() {
        if (DATABASE == null) {
            throw new ConfigurationException(
                    "database",
                    "Database has not been configured",
                    "Call DatabaseProvider.configure(database) at startup"
            );
        }
        return DATABASE;
    }

    /**
     * Checks if a database has been configured.
     *
     * @return true if configured
     */
    public static boolean isConfigured() {
        return DATABASE != null;
    }

    /**
     * Resets the database configuration. For testing only.
     */
    public static void reset() {
        DATABASE = null;
    }
}
