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
