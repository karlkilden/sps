package com.kildeen.sps.persistence;

public class DataBaseProvider {
    private static Database DATABASE;

    public static Database configure(Database database) {
        DATABASE = database;
        return DATABASE;
    }

    public static Database database() {
        return DATABASE;
    }
}
