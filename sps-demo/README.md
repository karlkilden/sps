# SPS Demo

End-to-end demonstration of the Simple Pub/Sub messaging system.

## Quick Start

```bash
# Build everything
make build

# Start the services
make up

# Send a test event
make demo

# Watch the logs
make logs
```

## Architecture

```
┌─────────────────┐     HTTP POST      ┌─────────────────┐
│   Publisher     │ ────────────────►  │   Subscriber    │
│   :8081         │   /receive-event   │   :8082         │
└─────────────────┘                    └─────────────────┘
        │                                      │
        │                                      │
        └──────────────┬───────────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │   PostgreSQL    │
              │   :5433         │
              └─────────────────┘
```

## Endpoints

### Publisher (port 8081)

```bash
# Publish an event
curl -X POST "http://localhost:8081/demo/publish?type=greeting_01" \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello World!"}'

# Health check
curl http://localhost:8081/demo/health
```

### Subscriber (port 8082)

Events are automatically delivered to the subscriber's `/receive-event/{type}` endpoint.

Check the logs to see received events:
```bash
make logs
```

## Make Targets

| Target       | Description                        |
|--------------|-----------------------------------|
| `make build` | Build JAR and Docker image        |
| `make up`    | Start all containers              |
| `make down`  | Stop all containers               |
| `make logs`  | Follow container logs             |
| `make demo`  | Send a test event                 |
| `make dev`   | Run with Quarkus hot-reload       |
| `make clean` | Clean everything                  |

## Files

```
sps-demo/
├── src/main/java/com/kildeen/sps/demo/
│   ├── DemoPublisherResource.java   # REST endpoint for publishing
│   ├── DemoInletResource.java       # REST endpoint for receiving
│   ├── DemoReceiver.java            # Business logic handler
│   └── SpsConfiguration.java        # CDI wiring
├── docker/
│   └── compose.yml                  # Docker Compose config
├── Dockerfile
├── Makefile
└── README.md
```
