package com.kildeen.sps.json;

import com.kildeen.sps.persistence.Database;

public class JsonProvider {
    private static Json JSON;

    public static Json configure(Json json) {
        JSON = json;
        return JSON;
    }

    public static Json json() {
        return JSON;
    }
}
