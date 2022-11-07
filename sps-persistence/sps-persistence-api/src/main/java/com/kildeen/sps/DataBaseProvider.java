package com.kildeen.sps;

public class DataBaseProvider {

    private static Database DATABASE;

    public void configure(Database database) {
        DATABASE = database;
    }

    public static Database database() {
        return DATABASE;
    }
}
