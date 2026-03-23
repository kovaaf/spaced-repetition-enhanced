# Spaced Repetition Data Service

gRPC statistics service for the Spaced Repetition Enhanced system. Provides analytics and data processing for spaced repetition learning data.

## Overview

The data service exposes gRPC endpoints for recording answer events and retrieving analytics. It stores events in a PostgreSQL database and supports time-range queries, user filtering, and quality distribution analysis.

## Architecture

- **gRPC serverProperties**: Port 50051 (configurable via `DATA_SERVICE_PORT`)
- **HTTP health endpoint**: Port 8081 (configurable via `SERVER_PORT`)
- **Database**: PostgreSQL with schema `public`
- **Connection pooling**: HikariCP with max 15 connections

## Service Endpoints

### gRPC Service `AnalyticsService`
- `RecordAnswerEvent` - Records a user's answer to a spaced repetition card
- `GetAnalytics` - Retrieves answer events with filtering by user, time range, and pagination
- `StreamAnalytics` - Server-side streaming of analytics events with periodic polling for new data

### Health Checks
- gRPC health check: `grpcProperties.health.v1.Health/Check`
- HTTP readiness probe: `GET http://localhost:8081/health/ready`
- HTTP liveness probe: `GET http://localhost:8081/health/live`

### Quality Enum Mapping

The quality values correspond to spaced repetition ratings:

| Value | Enum | Description |
|-------|------|-------------|
| 0 | AGAIN | Card was answered incorrectly |
| 3 | HARD | Card was answered correctly with difficulty |
| 4 | GOOD | Card was answered correctly |
| 5 | EASY | Card was answered correctly and easily |

## Database Schema

The service uses the following tables:

### `answer_events`
Stores individual answer events with quality rating (0, 3, 4, 5).

Columns:
- `event_id` (BIGSERIAL PRIMARY KEY)
- `user_id` (BIGINT) - References `user_info.user_chat_id`
- `deck_id` (BIGINT) - References `deck.deck_id`
- `card_id` (BIGINT) - References `card.card_id`
- `quality` (INTEGER) - CHECK constraint (0, 3, 4, 5)
- `event_timestamp` (TIMESTAMPTZ)
- `outbox_id` (BIGINT) - References `analytics_outbox` table for audit trail

**Note**: Uses BIGINT for IDs to match existing bot schema (technical debt acknowledged in TRD v2.2).

### Indexes
- `idx_user_timestamp` (user_id, event_timestamp)
- `idx_timestamp` (event_timestamp)
- `idx_user_quality` (user_id, quality)
- `idx_answer_events_outbox` (outbox_id)

## Configuration

Environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DATA_SERVICE_PORT` | gRPC serverProperties port | 50051 |
| `SERVER_PORT` | HTTP health endpoint port | 8081 |
| `POSTGRES_DB_HOST` | PostgreSQL host | localhost |
| `POSTGRES_DB_PORT` | PostgreSQL port | 5432 |
| `POSTGRES_DB_NAME` | Database name | spaced-repetition-bot-db |
| `POSTGRES_DB_USER` | Database user | postgres |
| `POSTGRES_DB_PASSWORD` | Database password | postgres |
| `DATA_SERVICE_DB_MAX_POOL_SIZE` | Connection pool size | 15 |
| `DATA_SERVICE_DB_SCHEMA` | Database schema | public |

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

**Production (`docker-compose.yml`)**:
```yaml
services:
  spaced-repetition-data:
    image: spaced-repetition-data:latest
    ports:
      - "50051:50051"  # gRPC port
      - "8081:8081"  # HTTP health endpoint
    depends_on:
      spaced-repetition-bot-db:
        condition: service_healthy
```

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

## Performance

- **Query performance**: GetAnalytics query for 10,000 records executes in <5ms (target: <500ms)
- **Throughput**: Sequential RecordAnswerEvent calls handle ≥100 events/second (meets target)
- **Concurrency**: Concurrent calls achieve ~578 events/second with 10 threads
- **Database**: Within 128MB container memory limit

See [Performance Testing](../docs/performance-testing.md) for detailed metrics and optimization recommendations.

## Monitoring

### Health Endpoints
- `GET /health/ready` - Readiness probe (checks database connectivity)
- `GET /health/live` - Liveness probe (service running)
- `GET /metrics` - Metrics endpoint (Prometheus-style with request/error counters)

### Logs
- Log level configurable via `LOGGING_LEVEL_ROOT` environment variable
- Default: INFO

## Development

### Adding New Endpoints

1. Add proto definition in `spaced-repetition-protobuf/src/main/proto/analytics.proto`
2. Generate Java classes via `./gradlew generateProto`
3. Implement service in `src/main/java/org/company/spacedrepetitiondata/service/`
4. Extend `AnalyticsServiceImplBase` and register in `DataServiceApplication`

### Testing

```bash
./gradlew test
```

Tests cover:
- Service layer validation and error handling
- Repository data access logic
- Health check components
- Metrics collection

Integration tests use TestContainers PostgreSQL (requires Docker).

## Troubleshooting

### Common Issues

1. **Database connection errors**: Verify PostgreSQL is running and environment variables are set correctly.
2. **Schema mismatch**: Ensure `DATA_SERVICE_DB_SCHEMA` is set to `public` (not `bot`).
3. **Port conflicts**: Check that ports 50051 and 8081 are not already in use.
4. **gRPC client errors**: Verify the service is running and the gRPC channel is configured correctly.

### Logs

View container logs:
```bash
docker logs spaced-repetition-data
```

## Related Modules

- [Spaced Repetition Bot](../spaced-repetition-bot/README.md) - Primary Telegram bot
- [Spaced Repetition UI](../spaced-repetition-ui/README.md) - Desktop analytics viewer
- [Spaced Repetition Protobuf](../spaced-repetition-protobuf/) - Protocol Buffer definitions