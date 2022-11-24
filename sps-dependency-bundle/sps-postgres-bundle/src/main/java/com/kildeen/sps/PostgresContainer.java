package com.kildeen.sps;

import org.jdbi.v3.core.Jdbi;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresContainer extends PostgreSQLContainer<PostgresContainer> {
    public static final PostgresContainer INSTANCE = new PostgresContainer().withDatabaseName("test")
            .withUsername("postgres")
            .withPassword("postgres");
    private static final Jdbi jdbi;

    static {
        INSTANCE.start();

        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setDatabaseName("test");
        ds.setUser("postgres");
        ds.setPassword("postgres");
        ds.setUrl(PostgresContainer.INSTANCE.getJdbcUrl());
        jdbi = Jdbi.create(ds);
    }

    public PostgresContainer() {
        super("postgres:latest");
    }

    public Jdbi jdbi() {
        return jdbi;
    }
}
