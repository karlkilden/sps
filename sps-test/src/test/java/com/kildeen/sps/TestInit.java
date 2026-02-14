package com.kildeen.sps;

import com.kildeen.sps.json.JsonProvider;
import com.kildeen.sps.json.JacksonJson;
import org.slf4j.simple.SimpleLogger;

public class TestInit {
    static {
        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, Boolean.TRUE.toString());
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss:SSS");
        JsonProvider.configure(new JacksonJson());
    }
    public static void init() {
    }
}
