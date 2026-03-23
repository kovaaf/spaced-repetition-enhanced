# Spaced Repetition UI

Java Swing desktop application for visualizing spaced repetition analytics. Connects to the gRPC data service to display answer statistics, quality distributions, and user performance over time.

## Overview

The UI module provides a graphical interface for viewing analytics data collected by the Spaced Repetition system. It follows the TRD Appendix E layout specification with filter panels, statistics tables, and visualization components.

## Features

- **Time-range filtering**: Select predefined periods (Last Week, Last Month, Last Year, All Time)
 **Column sorting**: Click column headers to sort data
 **Date sorting**: Proper chronological sorting with most recent events first by default


## Getting Started

### Prerequisites

- Java 17 or higher
- Running instance of Spaced Repetition Data Service (port 50051)
- PostgreSQL database (shared with bot and data service)

### Building

```bash
cd spaced-repetition-ui
mvn clean compile
```

### Running

#### Option 1: With Docker Compose (Recommended)
Start all services including data service:
```bash
# Development
docker-compose -f docker-compose-dev.yml up -d

# Production
docker-compose up -d
```

Then run the UI application:
```bash
cd spaced-repetition-ui
mvn clean package -DskipTests
java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
```

**Important**: For development (port 50051), you must edit `application.yml` to use `localhost:50051` before building the JAR.

#### Option 2: Standalone with Existing Data Service
If data service is already running:

1. **Configure the correct port** in `application.yml`:
   - Development (port 50051): `data.service.url=localhost:50051`

2. Build and run UI:
```bash
cd spaced-repetition-ui
mvn clean package -DskipTests
java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
```

**Note**: System property overrides (`-Ddata.service.url`) are **NOT** supported. You must edit `application.yml` directly.

#### Option 3: From IDE
Run `GUIRunner.main()` (entry point class) from your IDE.

## Configuration

### Data Service Connection
The UI connects to the data service using configuration in `application.yml`:

**Port Configuration**:
- **Production**: Port 50051 (data service production port in `docker-compose.yml`)
- **Development**: Port 50051 (data service default development port in `docker-compose-dev.yml`)

**Important Configuration Notes**:
1. **Configuration Source**: The UI reads **ONLY** from `application.yml` file. System properties (`-Ddata.service.url`) and environment variables are **NOT** supported.
2. **Default Behavior**: If `application.yml` is not found or cannot be parsed, the UI defaults to `localhost:50051`.
3. **Development Setup**: For development with `docker-compose-dev.yml`, you **MUST** edit `application.yml`:
   ```properties
   # Change from 50051 to 50051 for development
   data.service.url=localhost:50051
   ```
4. **Production Setup**: For production with `docker-compose.yml`, use the default `localhost:50051`.

### Configuration Workflow

#### For Development (port 50051):
1. Edit `spaced-repetition-ui/src/main/resources/application.yml`:
   ```properties
   data.service.url=localhost:50051
   ```
2. Rebuild the JAR:
   ```bash
   mvn clean package -DskipTests
   ```
3. Run the UI:
   ```bash
   java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
   ```

### Headless Mode for Testing
For running tests or in CI/CD environments:
```bash
java -Djava.awt.headless=true -jar spaced-repetition-ui.jar
```

## User Interface

### Interactions

- **Period filter**: Changes immediately trigger data refresh (Last Week, Last Month, Last Year, All Time)
- **User filter**: Changes immediately trigger data refresh (All Users or specific user)
- **View toggle**: Currently shows table view (chart view planned)
 **Column sorting**: Click any column header to sort ascending/descending


## Related Modules

- [Spaced Repetition Data Service](../spaced-repetition-data/README.md) - gRPC analytics service
