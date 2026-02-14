package com.kildeen.sps.schemagen;

import static java.lang.invoke.MethodHandles.lookup;

import com.kildeen.sps.inlet.Inlet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SchemagenHealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final Inlet inlet;

    @Inject
    public SchemagenHealthCheck(Inlet inlet) {
        this.inlet = inlet;
    }

    @Liveness
    public HealthCheck liveness() {
        return () -> HealthCheckResponse.up("sps-schemagen-live");
    }

    @Readiness
    public HealthCheck readiness() {
        return () -> {
            HealthCheckResponseBuilder builder = HealthCheckResponse.named("sps-schemagen-ready");

            try {
                if (inlet != null) {
                    builder.up()
                            .withData("inlet", "initialized");
                } else {
                    builder.down()
                            .withData("inlet", "not initialized");
                }
            } catch (Exception e) {
                LOG.error("Readiness check failed", e);
                builder.down()
                        .withData("error", e.getMessage());
            }

            return builder.build();
        };
    }
}
