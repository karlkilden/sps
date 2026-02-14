package com.kildeen.sps.persistence.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Factory for creating PostgresDatabase instances with proper connection pooling and migrations.
 */
public class PostgresDatabaseFactory {
    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maxPoolSize;

    private HikariDataSource dataSource;
    private Jdbi jdbi;
    private PostgresDatabase database;

    public PostgresDatabaseFactory(String jdbcUrl, String username, String password) {
        this(jdbcUrl, username, password, 10);
    }

    public PostgresDatabaseFactory(String jdbcUrl, String username, String password, int maxPoolSize) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Creates the database, runs migrations, and returns the PostgresDatabase instance.
     */
    public PostgresDatabase create() {
        LOG.info("Initializing PostgreSQL database at {}", jdbcUrl);

        migrate();
        initConnectionPool();

        database = new PostgresDatabase(jdbi);
        LOG.info("PostgreSQL database initialized successfully");

        return database;
    }

    /**
     * Runs Flyway migrations.
     */
    private void migrate() {
        LOG.info("Running database migrations");

        HikariDataSource migrationDs = createDataSource(jdbcUrl, username, password, 1);
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(migrationDs)
                    .locations("classpath:db/migration")
                    .load();

            int applied = flyway.migrate().migrationsExecuted;
            LOG.info("Applied {} migrations", applied);
        } finally {
            migrationDs.close();
        }
    }

    /**
     * Initializes the connection pool.
     */
    private void initConnectionPool() {
        LOG.info("Initializing connection pool with max size {}", maxPoolSize);

        dataSource = createDataSource(jdbcUrl, username, password, maxPoolSize);
        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new PostgresPlugin());
    }

    /**
     * Creates a HikariCP data source.
     */
    private HikariDataSource createDataSource(String url, String user, String pass, int poolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(poolSize);
        config.setPoolName("sps-postgres-pool");
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    /**
     * Gets the underlying JDBI instance for direct database access.
     */
    public Jdbi getJdbi() {
        return jdbi;
    }

    /**
     * Shuts down the connection pool.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOG.info("Shutting down database connection pool");
            dataSource.close();
        }
    }

    /**
     * Builder for PostgresDatabaseFactory.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String jdbcUrl;
        private String username;
        private String password;
        private int maxPoolSize = 10;

        public Builder withJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public PostgresDatabaseFactory build() {
            if (jdbcUrl == null || username == null || password == null) {
                throw new IllegalStateException("jdbcUrl, username, and password are required");
            }
            return new PostgresDatabaseFactory(jdbcUrl, username, password, maxPoolSize);
        }
    }
}
