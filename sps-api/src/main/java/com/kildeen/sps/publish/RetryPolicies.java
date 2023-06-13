package com.kildeen.sps.publish;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparing;

public record RetryPolicies(List<RetryPolicy> retryPolicies, List<RetryPolicy> defaultPolicies) {

    public static final List<RetryPolicy> DEFAULT_RETRY_POLICIES = List.of(
            RetryPolicy.newBuilder()
                    .withMaxRetries(4)
                    .withWaitInMs(2000)
                    .withDeliveryType(DeliveryType.DATABASE)
                    .build(),

            RetryPolicy.newBuilder()
                    .withMaxRetries(9)
                    .withWaitInMs(20000)
                    .withDeliveryType(DeliveryType.DATABASE)
                    .build()
    );


    public RetryPolicy forAttempt(int attempt, List<DeliveryType> allowedDeliveryTypes) {
        HashSet<DeliveryType> deliveryTypes = new HashSet<>(allowedDeliveryTypes);
        return firstMatch(retryPolicies, attempt, deliveryTypes)
                .orElse(defaultPolicy(attempt, deliveryTypes));
    }

    private Optional<RetryPolicy> firstMatch(List<RetryPolicy> retryPolicies,
                                             int attempt,
                                             Set<DeliveryType> allowedDeliveryTypes) {
        return retryPolicies.stream()
                .filter(retryPolicy -> allowedDeliveryTypes.contains(retryPolicy.deliveryType()))
                .filter(retryPolicy -> addIfFitsRange(attempt, retryPolicy))
                .filter(retryPolicy -> attempt < retryPolicy.maxRetries)
                .min(comparing(policy -> attempt - policy.attemptStartInclusive + policy.attemptEndInclusive));
    }


    private RetryPolicy defaultPolicy(int attempt, Set<DeliveryType> allowedDeliveryTypes) {
        return firstMatch(defaultPolicies, attempt, allowedDeliveryTypes)
                .orElse(null);

    }

    private boolean addIfFitsRange(int attempt, RetryPolicy retryPolicy) {
        return retryPolicy.attemptStartInclusive <= attempt && retryPolicy.attemptEndInclusive >= attempt;
    }

    public static class RetryPolicy {
        private final int attemptStartInclusive;
        private final int attemptEndInclusive;
        private final int maxRetries;
        private final RetentionType retention;
        private final boolean reloadSubscription;
        private final long abandonEventAfterMs;
        private final int minRetries;
        private final int waitInMs;
        private final DeliveryType deliveryType;

        private RetryPolicy(Builder builder) {
            this.attemptStartInclusive = builder.attemptStartInclusive;
            this.attemptEndInclusive = builder.attemptEndInclusive;
            this.maxRetries = builder.maxRetries;
            this.retention = builder.retention;
            this.reloadSubscription = builder.reloadSubscription;
            this.abandonEventAfterMs = builder.abandonEventAfterMs;
            this.minRetries = builder.minRetries;
            this.waitInMs = builder.waitInMs;
            this.deliveryType = builder.deliveryType;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public int attemptStartInclusive() {
            return attemptStartInclusive;
        }

        public int attemptEndInclusive() {
            return attemptEndInclusive;
        }

        public int maxRetries() {
            return maxRetries;
        }

        public RetentionType retention() {
            return retention;
        }

        public boolean refreshSubscription() {
            return reloadSubscription;
        }

        public long abandonEventAfterMs() {
            return abandonEventAfterMs;
        }

        public int minRetries() {
            return minRetries;
        }

        public int waitInMs() {
            return waitInMs;
        }

        public DeliveryType deliveryType() {
            return deliveryType;
        }

        public enum RetentionType {
            IN_MEMORY, PERSISTENT
        }

        public static final class Builder {
            private int maxRetries;
            private int attemptStartInclusive;
            private int attemptEndInclusive;
            private RetentionType retention;
            private boolean reloadSubscription;
            private long abandonEventAfterMs;
            private int minRetries;
            private int waitInMs;
            private DeliveryType deliveryType;

            private Builder() {
            }

            public Builder withMaxRetries(int maxRetries) {
                this.maxRetries = maxRetries;
                return this;
            }

            public Builder withForRetryAttempt(int attemptStartInclusive, int attemptEndInclusive) {
                this.attemptStartInclusive = attemptStartInclusive;
                this.attemptEndInclusive = attemptEndInclusive;
                return this;
            }

            public Builder withRetention(RetentionType retention) {
                this.retention = retention;
                return this;
            }

            public Builder withReloadSubscription(boolean reloadSubscription) {
                this.reloadSubscription = reloadSubscription;
                return this;
            }

            public Builder withAbandonEventAfterMs(long abandonEventAfterMs) {
                this.abandonEventAfterMs = abandonEventAfterMs;
                return this;
            }

            public Builder withMinRetries(int minRetries) {
                this.minRetries = minRetries;
                return this;
            }

            public Builder withWaitInMs(int waitInMs) {
                this.waitInMs = waitInMs;
                return this;
            }

            public Builder withDeliveryType(DeliveryType deliveryType) {
                this.deliveryType = deliveryType;
                return this;
            }

            public RetryPolicy build() {
                if (maxRetries == 0) {
                    maxRetries = attemptEndInclusive;
                }
                if (retention == null) {
                    retention = RetentionType.IN_MEMORY;
                }
                if (deliveryType == null) {
                    deliveryType = DeliveryType.HTTP;
                }
                return new RetryPolicy(this);
            }
        }
    }
}
