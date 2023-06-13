package com.kildeen.sps.publish;

import java.util.List;

public record PublishPolicy(List<DeliveryType> acceptedDeliveryTypes) {
}
