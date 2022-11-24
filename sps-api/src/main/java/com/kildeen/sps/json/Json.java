package com.kildeen.sps.json;


public interface Json {

    <T> T readValue(String json, Class<T> clazz);

    byte[] writeValueAsBytes(Object object);

    String write(Object object);

}
