package com.kildeen.sps.json;

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
