# Spaced Repetition Data Service

gRPC statistics service for the Spaced Repetition Enhanced system. Provides analytics and data processing for spaced repetition learning data.

## Overview

The data service exposes gRPC endpoints for recording answer events and retrieving analytics. It stores events in a PostgreSQL database and supports time-range queries, user filtering, and quality distribution analysis.

## Building and Running

### Local Development

```bash
cd spaced-repetition-data
./gradlew build
./gradlew run
```

### Docker

```bash
# Build image
docker build -t spaced-repetition-data .

# Run container (default port 50051 for gRPC, 8081 for HTTP health)
docker run -p 50051:50051 -p 8081:8081 spaced-repetition-data

# Run with custom port (e.g., production port 50051)
docker run -p 50051:50051 -p 8081:8081 -e DATA_SERVICE_PORT=50051 spaced-repetition-data
```

### Docker Compose

The service is included in the main `docker-compose.yml` (production) and `docker-compose-dev.yml` (development):

**Development (`docker-compose-dev.yml`)**:
```yaml
services:
  spaced-repetition-data:
    build: ./spaced-repetition-data
    ports:
      - "50051:50051"  # gRPC port (development default)
      - "8081:8081"  # HTTP health endpoint
    environment:
      DATA_SERVICE_PORT: ${DATA_SERVICE_PORT:-50051}
    depends_on:
      spaced-repetition-bot-db:
        condition: service_healthy
```

## Monitoring

### Health Endpoints
- `GET /health/ready` - Readiness probe (checks database connectivity)
- `GET /health/live` - Liveness probe (service running)
- `GET /metrics` - Metrics endpoint (Prometheus-style with request/error counters)

## Related Modules

- [Spaced Repetition UI](../spaced-repetition-ui/README.md) - Desktop analytics viewer