package com.kildeen.sps.inlet;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class HealthCheckReceiver implements Receiver {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
        public void receive(SpsEvent spsEvent) {
            int severity = (int) spsEvent.data().get("severity");
            String message = (String) spsEvent.data().get("message");
            if (severity == 1) {
                LOG.info("SPS health {}", message);
            }
            if (severity == 2) {
                LOG.warn("SPS health {}", message);
            }
            if (severity == 3) {
                LOG.error("SPS health {}", message);
            }
        }

        @Override
        public String eventType() {
            return SpsEventType.healthcheck_01.toString();
        }

}
