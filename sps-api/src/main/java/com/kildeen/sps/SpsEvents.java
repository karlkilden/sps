package com.kildeen.sps;

import java.util.List;
import java.util.Map;

public record SpsEvents(String eventType, List<SpsEvent> spsEvents) {
}
