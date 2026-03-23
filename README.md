![spaced-repetition-bot-coverage](https://gist.githubusercontent.com/kovaaf/c27f99167289d841c193513031ab9a48/raw/jacoco_spaced_repetition_bot.svg)
![spaced-repetition-data-coverage](https://gist.githubusercontent.com/kovaaf/c27f99167289d841c193513031ab9a48/raw/jacoco_spaced_repetition_data.svg)

## Modules Overview

Spaced Repetition Enhanced is a multi-module system for spaced repetition learning with Telegram bot integration, analytics processing, and visualization:

- **spaced-repetition-bot**: Spring Boot Telegram bot with PostgreSQL database and spaced repetition scheduling
- **spaced-repetition-data**: gRPC statistics service for analytics and answer event processing
- **spaced-repetition-ui**: Java Swing desktop application for analytics visualization
- **spaced-repetition-protobuf**: Protocol Buffer definitions for gRPC communication
- **spaced-repetition-dto**: Shared data transfer objects (placeholder)

The system automatically generates learning cards from Git repositories, provides spaced repetition scheduling through a Telegram interface, and offers analytics via gRPC API and desktop UI.

## Quick Start

### Development (builds from source using Dockerfile)
```bash
docker-compose -f docker-compose-dev.yml up -d
```

### Production (uses non-dev configuration with pre-built images from Docker Hub)
```bash
docker-compose up -d
```

**Key difference**: 
- `docker-compose-dev.yml` builds from the local `Dockerfile` (includes tests, development settings)
- `docker-compose.yml` uses pre-built image (used in GitHub workflow for production)

**Default port mappings** (when using Docker):
- PostgreSQL database: `localhost:5432` → `5432` (container)
- Bot service: `localhost:8080` → `8080` (container)
- Data service (gRPC): `localhost:50051` → `50051` (container)
- Data service (HTTP health): `localhost:8081` → `8081` (container)

## Telegram Bot Setup

To use the bot, you need a Telegram bot token:

1. Open Telegram and search for `@BotFather`
2. Start conversation with `/start` command
3. Create new bot: `/newbot`
4. Choose bot name (e.g., "My Spaced Repetition Bot")
5. Choose bot username (must end with 'bot', e.g., `my_spaced_repetition_bot`)
6. Copy the API token provided

**Security note**: Never commit bot tokens to version control.

## Environment Configuration

Create a `.env` file in the project root with these variables:

```bash
# Required: Telegram Bot Configuration
TELEGRAM_BOT_NAME='Your Bot Name'
TELEGRAM_BOT_TOKEN='your_token_from_botfather'

# Required for Git integration: Connects to repository for default deck generation
DEFAULT_DECK_REPO_URL='your_repository_url_here'
GIT_TOKEN='your_github_token'
GIT_WEBHOOK_SECRET='your_webhook_secret'
GIT_SSH_PASSPHRASE='your_ssh_passphrase'

# Database Configuration (optional - defaults shown)
POSTGRES_DB_USER=postgres
DB_PASSWORD=postgres
POSTGRES_DB_HOST=localhost

# Encryption: Used for sensitive data encryption in SecurityConfig
ENCRYPTION_PASSWORD='encryption_password'
ENCRYPTION_SALT='encryption_salt'

# Analytics Data Service Configuration (optional - defaults shown)
DATA_SERVICE_URL='static://spaced-repetition-data:50051'
ANALYTICS_OUTBOX_PROCESSOR_BATCH_SIZE=200
ANALYTICS_OUTBOX_PROCESSOR_CRON='* * * * * *'
```

## Custom Repository Configuration

The default deck is automatically generated from a Git repository. To use your own repository:

1. Set the `DEFAULT_DECK_REPO_URL` environment variable in your `.env` file to point to your repository
2. Optionally edit `spaced-repetition-bot/src/main/resources/application.yml` to:
   - Adjust the `source-folders` list to specify which folders contain cards
   - Modify `app.default-deck.name` for your deck name

Example `.env` configuration:
```bash
DEFAULT_DECK_REPO_URL='https://github.com/yourusername/your-repo.git'
```

Example `application.yml` modifications:
```yaml
app:
  default-deck:
    name: "My Custom Deck"
    repo:
      source-folders:
        - my-cards/java
        - my-cards/algorithms
```

## GitHub Workflow Integration

The project includes CI/CD automation:

- `docker-compose.yml` is used in the GitHub workflow for production deployment
- Images are built and pushed to Docker Hub automatically on commits to main
- Production deployments use pre-built images from Docker Hub

## Git Integration Purpose

The Git environment variables (`DEFAULT_DECK_REPO_URL`, `GIT_TOKEN`, `GIT_WEBHOOK_SECRET`, `GIT_SSH_PASSPHRASE`) connect to the repository from which the default deck is automatically generated. The bot syncs cards from specified folders in this repository every 30 minutes.

## Encryption Configuration

Encryption is configured using Spring Security's `TextEncryptor` in `SecurityConfig.java`. It uses `ENCRYPTION_PASSWORD` and `ENCRYPTION_SALT` environment variables for encrypting sensitive data.

## Analytics and Visualization

The system includes a comprehensive analytics pipeline:

### Data Service
- **gRPC API**: Port 50051 for recording answer events, retrieving analytics, and streaming analytics with periodic polling
- **Health checks**: HTTP endpoint on port 8081 (`/health/ready`, `/health/live`)
- **Database**: Shares PostgreSQL instance with bot service
- **Performance**: Handles ≥100 events/second, queries respond in <5ms for 10k records

### Desktop UI
- **Java Swing application**: Visualizes answer statistics and quality distributions
- **Filtering**: By time period, user, and view type
- **Data source**: Connects to data service gRPC endpoint

### Environment Variables
Add these to your `.env` file for analytics functionality:

```bash
# Data Service Configuration
DATA_SERVICE_PORT=50051
DATA_SERVICE_DB_SCHEMA=public
DATA_SERVICE_DB_MAX_POOL_SIZE=15

# UI Configuration (optional)
DATA_SERVICE_HOST=localhost
```

### Transactional Outbox Pattern
The analytics pipeline uses a transactional outbox pattern for reliable event delivery:

1. **Event capture**: Answer events stored in `analytics_outbox` table within bot transaction
2. **Background processing**: `OutboxProcessor` runs every second, processes up to 200 events per batch
3. **Reliable delivery**: Events retried with exponential backoff (1s, 2s, 4s) on failures
4. **Dead letter queue**: Failed events moved to `analytics_dlq` after 5 retries

**Configuration**:
- Batch size: `ANALYTICS_OUTBOX_PROCESSOR_BATCH_SIZE` (default: 200)
- Processing frequency: `ANALYTICS_OUTBOX_PROCESSOR_CRON` (default: every second)
- Max retries: 5 with exponential backoff

### Quick Start with Analytics

#### Option 1: Complete Docker Deployment
```bash
# Start all services (bot, data service, database)
docker-compose up -d

# Verify data service is healthy
curl -f http://localhost:8081/health/ready

# Build and run UI application
cd spaced-repetition-ui
mvn clean package -DskipTests
java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
```

#### Option 2: Development Environment
```bash
# Start services with development configuration
docker-compose -f docker-compose-dev.yml up -d

# Configure UI for development (port 50051)
# Edit spaced-repetition-ui/src/main/resources/application.properties:
# data.service.url=localhost:50051

# Build and run UI
cd spaced-repetition-ui
mvn clean package -DskipTests
java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
```

**Note**: For development, you must edit `application.properties` to use `localhost:50051` before building the JAR.

#### Option 3: UI Only (Existing Data Service)
If data service is already running:

1. **Configure the correct port** in `application.properties`:
   - Edit `spaced-repetition-ui/src/main/resources/application.properties`
   - Set `data.service.url=localhost:50051` for production (port 50051)
   - Set `data.service.url=localhost:50051` for development (port 50051)

2. Build and run UI:
```bash
cd spaced-repetition-ui
mvn clean package -DskipTests
java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
```

**Important**: System property overrides (`-Ddata.service.url`) are **NOT** supported. You must edit `application.properties` directly.

**Default Port Mappings**:
- Data Service gRPC: 50051 (production), 50051 (development)
- Data Service HTTP Health: 8081
- Bot Service: 8080
- PostgreSQL Database: 5432

## Additional Resources

- [Analytics API Documentation](docs/analytics-api.md) - gRPC API reference
- [Performance Testing Results](docs/performance-testing.md) - Performance metrics and optimization
- [Spaced Repetition UI](spaced-repetition-ui/README.md) - Desktop application guide
- [Spaced Repetition Data Service](spaced-repetition-data/README.md) - Analytics service documentation
- [Troubleshooting Guide](docs/troubleshooting.md) - Common issues and solutions
- [End-to-End Test Suite](docs/e2e-test-suite.md) - Integration testing documentation