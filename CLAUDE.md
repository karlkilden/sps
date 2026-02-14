# SPS - Simple Pub/Sub

A Java-based publish/subscribe messaging system.

## Build & Test

```bash
mvn clean install       # Build all modules
mvn test                # Run tests
mvn test -pl sps-test   # Run integration tests only
```

## Architecture

Multi-module Maven project (Java 21) with hexagonal architecture:
- **api** modules define contracts/interfaces
- **core** modules contain business logic
- **adapter** modules implement integrations
- **deployment** modules are runnable services

### Key Modules

| Module | Purpose |
|--------|---------|
| `sps-api` | Core interfaces: `SpsEvent`, `Publish`, `Receiver`, `Database` |
| `sps-publish` | Event publishing with retry policies and rate limiting |
| `sps-subscribe` | Subscription management |
| `sps-inlet` | Event receiving/consuming |
| `sps-schemagen` | Schema generation and registration |
| `sps-persistence` | Database layer (PostgreSQL + EmbeddedDatabase for tests) |
| `sps-json` | JSON serialization (Jackson) |
| `sps-client` | Client-side resilience (`ClientCircuitBreaker`) |

### Core Concepts

- **SpsEvent**: Event with `type()`, `id()`, and `data()` payload
- **Receiver**: Handles incoming events for a specific `eventType()`
- **RetryPolicy**: Configurable retry tiers with backoff
- **ClientCircuitBreaker**: Client-side circuit breaker for resilience (in `sps-client`)
- **RateLimiter**: Server-side rate limiting per subscriber

### Transport & Delivery

- **Client**: Interface for event delivery (`post()` returns `CompletableFuture<IdWithReceiptsResult>`)
- **DeliveryType**: Enum with `HTTP` and `DATABASE` transport types
- **PublishPolicy**: Holds list of `acceptedDeliveryTypes` per subscriber (default: HTTP, DATABASE)
- **Sender**: Orchestrates delivery with fallback on connection failures
- **HttpClient**: Default HTTP transport in `sps-publish-adapter`
- **DatabaseClient**: Database fallback transport (writes to `sps_transport_queue`)
- **TransportQueuePoller**: Polls database for events when enabled via `.withTransportPolling(true)`

### Transport Fallback Flow

1. Publisher tries HTTP delivery first (via `HttpClient`)
2. On connection failure (exception), falls back to DATABASE delivery
3. `DatabaseClient` inserts event into `sps_transport_queue` table
4. Subscriber's `TransportQueuePoller` polls queue and delivers to local `Receiver`
5. Events are marked as processed after successful delivery

### Key Files for Transport

| File | Purpose |
|------|---------|
| `sps-api/.../Client.java` | Transport client interface |
| `sps-api/.../DeliveryType.java` | HTTP, DATABASE enum |
| `sps-api/.../PublishPolicy.java` | Delivery type preferences |
| `sps-api/.../TransportQueueEntry.java` | Queue entry record |
| `sps-publish-core/.../Sender.java` | Orchestrates delivery with fallback |
| `sps-publish-adapter/.../HttpClient.java` | HTTP transport implementation |
| `sps-publish-adapter/.../DatabaseClient.java` | Database fallback transport |
| `sps-inlet-adapter/.../TransportQueuePoller.java` | Polls DB queue for events |

### Database Schema for Transport

```sql
-- V2__transport_queue.sql
CREATE TABLE sps_transport_queue (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    subscriber_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    UNIQUE(event_id, subscriber_id)
);
```

## Code Conventions

- Interfaces marked `@Contract` are stable public APIs
- `@Internal` marks implementation details
- Module naming: `sps-{domain}/sps-{domain}-{layer}`
- Tests use `EmbeddedDatabase` (in-memory) via `DatabaseProvider`

## Running Locally

```bash
# Start PostgreSQL
cd docker && docker-compose up -d

# Run schemagen service
mvn exec:java -pl sps-schemagen/sps-schemagen-deployment

# Run subscribe service
mvn exec:java -pl sps-subscribe/sps-subscribe-deployment
```
