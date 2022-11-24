package com.kildeen.sps.persistence;

public record Config(SchemaGen gen) {

    public record SchemaGen(String url) {
    }
}
