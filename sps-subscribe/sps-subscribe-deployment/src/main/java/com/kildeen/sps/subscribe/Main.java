package com.kildeen.sps.subscribe;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Main entry point for the Subscribe service.
 * This is a Quarkus application - start with:
 *   mvn quarkus:dev (development mode)
 *   java -jar target/quarkus-app/quarkus-run.jar (production)
 */
@QuarkusMain
public class Main {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
