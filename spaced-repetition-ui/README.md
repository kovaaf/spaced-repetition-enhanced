# Spaced Repetition UI

Java Swing desktop application for visualizing spaced repetition analytics. Connects to the gRPC data service to display answer statistics, quality distributions, and user performance over time.

## Overview

The UI module provides a graphical interface for viewing analytics data collected by the Spaced Repetition system. It follows the TRD Appendix E layout specification with filter panels, statistics tables, and visualization components.

## Features

- **Time-range filtering**: Select predefined periods (Last Week, Last Month, Last Year, All Time)
- **User selection**: Filter by specific users or view all users
- **View toggles**: Switch between table view and chart visualization (table implemented, chart planned)
 **Error handling**: Graceful degradation when data service is unavailable
 **Column sorting**: Click column headers to sort data
 **Quality enum display**: Shows human-readable quality names (AGAIN, HARD, GOOD, EASY)
 **Date sorting**: Proper chronological sorting with most recent events first by default


## Architecture

- **Plain Java Swing**: No Spring Boot dependencies
- **gRPC client**: Communicates with data service on port 9090
- **SwingWorker**: Background data fetching to avoid UI blocking
- **MVC pattern**: Separation of UI components, business logic, and data fetching

## Components

### Main Frame (`MainFrame.java`)
Primary application window with three regions:
- **NORTH**: Filter panel (period, user, view toggle)
- **CENTER**: Statistics table display
- **SOUTH**: Status bar

### Filter Components
- `PeriodFilter`: Toggle button group (Last Week, Last Month, Last Year, All Time)
- `UserFilter`: Dropdown for user selection (All Users, User 1, User 2, etc.)
- `ViewToggle`: Toggle button group (Table, Chart - chart view planned)

### Data Fetching
- `StatisticsDataFetcher`: Coordinates data retrieval using `SwingWorker`, converts filter selections to gRPC parameters
- `AnalyticsServiceClient`: gRPC client wrapper with retry logic (max 3 attempts with exponential backoff)

## Getting Started

### Prerequisites

- Java 17 or higher
- Running instance of Spaced Repetition Data Service (port 9090)
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
# Development (port 50051)
docker-compose -f docker-compose-dev.yml up -d

# Production (port 9090)  
docker-compose up -d
```

Then run the UI application:
```bash
cd spaced-repetition-ui
mvn clean package -DskipTests
java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
```

**Important**: For development (port 50051), you must edit `application.properties` to use `localhost:50051` before building the JAR.

#### Option 2: Standalone with Existing Data Service
If data service is already running:

1. **Configure the correct port** in `application.properties`:
   - Development (port 50051): `data.service.url=localhost:50051`
   - Production (port 9090): `data.service.url=localhost:9090`

2. Build and run UI:
```bash
cd spaced-repetition-ui
mvn clean package -DskipTests
java -jar target/spaced-repetition-ui-0.0.1-SNAPSHOT.jar
```

**Note**: System property overrides (`-Ddata.service.url`) are **NOT** supported. You must edit `application.properties` directly.

#### Option 3: From IDE
Run `StatisticsViewer.main()` (entry point class) from your IDE.

## Configuration

### Data Service Connection
The UI connects to the data service using configuration in `application.properties`:

```properties
# Default production configuration (port 9090)
data.service.url=localhost:9090
```

**Port Configuration**:
- **Production**: Port 9090 (data service production port in `docker-compose.yml`)
- **Development**: Port 50051 (data service default development port in `docker-compose-dev.yml`)

**Important Configuration Notes**:
1. **Configuration Source**: The UI reads **ONLY** from `application.properties` file. System properties (`-Ddata.service.url`) and environment variables are **NOT** supported.
2. **Default Behavior**: If `application.properties` is not found or cannot be parsed, the UI defaults to `localhost:50051`.
3. **Development Setup**: For development with `docker-compose-dev.yml`, you **MUST** edit `application.properties`:
   ```properties
   # Change from 9090 to 50051 for development
   data.service.url=localhost:50051
   ```
4. **Production Setup**: For production with `docker-compose.yml`, use the default `localhost:9090`.

### Configuration Workflow

#### For Development (port 50051):
1. Edit `spaced-repetition-ui/src/main/resources/application.properties`:
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

#### For Production (port 9090):
1. Ensure `application.properties` has:
   ```properties
   data.service.url=localhost:9090
   ```
2. Build and run as usual.

**Note**: Unlike typical Java applications, this UI does **NOT** support system property overrides (`-Ddata.service.url`). You must modify the `application.properties` file directly.

### Headless Mode for Testing
For running tests or in CI/CD environments:
```bash
java -Djava.awt.headless=true -jar spaced-repetition-ui.jar
```

## User Interface

### Layout

The application follows TRD Appendix E layout with three panels:

```
┌─────────────────────────────────────────────────────────┐
│ Filters                                                 │
│ [●Last Week] [●Last Month] [○Last Year] [○All Time]    │
│ [All Users ▼] [●Table] [○Chart] [Apply Filters]        │
├─────────────────────────────────────────────────────────┤
│ Statistics                                              │
│ ┌──────────────────────────────────────────────────┐   │
│ │ User    | Deck         | Card    | Quality | Date│   │
│ │--------------------------------------------------│   │
│ │ User 1  | Default Deck | Card 101| GOOD (4)| ... │   │
│ │ User 2  | Java Concepts| Card 202| HARD (3)| ... │   │
│ └──────────────────────────────────────────────────┘   │
│ Placeholder data - TODO: Connect to data service       │
├─────────────────────────────────────────────────────────┤
│ Ready - Connected to data service: localhost:9090 [↻]  │
└─────────────────────────────────────────────────────────┘
```

**Table Columns**:
- **User**: User identifier
- **Deck**: Deck name
- **Card**: Card identifier/title
- **Quality**: Quality rating with enum name (AGAIN=0, HARD=3, GOOD=4, EASY=5)
 **Date**: ISO format date (yyyy-MM-dd HH:mm) with chronological sorting (most recent first by default)

### Interactions

- **Period filter**: Changes immediately trigger data refresh (Last Week, Last Month, Last Year, All Time)
- **User filter**: Changes immediately trigger data refresh (All Users or specific user)
- **View toggle**: Currently shows table view (chart view planned)
 **Column sorting**: Click any column header to sort ascending/descending


## Development

### Adding New Features

1. **New filter component**:
   - Create class in `components/filters/`
   - Implement filter interface
   - Add to `MainFrame` layout

2. **New visualization**:
   - Create panel in `components/charts/`
   - Implement data binding
   - Add to view toggle

3. **Additional data fields**:
   - Update `AnalyticsServiceClient` to fetch new fields
   - Modify table model in `MainFrame`
   - Update filter components as needed

### Code Style

- Follow Java Swing conventions
- Use `SwingUtilities.invokeLater()` for UI updates
- Keep long-running operations off the Event Dispatch Thread
- Use meaningful variable names and comments

## Testing

### Unit Tests

```bash
mvn test
```

Tests cover:
- `AnalyticsServiceClient`: gRPC client with retry logic
- `StatisticsDataFetcher`: Data fetching and filter translation
- `MainFrameIntegrationTest`: UI component interactions using AssertJ-Swing

### Integration Testing

Integration tests verify UI components work together with mocked gRPC client using AssertJ-Swing:

```bash
# Run all integration tests
mvn test -Dtest="*IntegrationTest"

# Run with headless mode (for CI/CD)
mvn test -Dtest="*IntegrationTest" -Djava.awt.headless=true
```

**Test Coverage**:
- `MainFrameIntegrationTest`: UI component interactions, filter behavior, table updates
- Uses AssertJ-Swing for GUI testing with mocked `AnalyticsServiceClient`
 Tests filter interactions, data fetching, and error handling

## Troubleshooting

### Common Issues

1. **Connection refused**: Ensure data service is running on the expected host/port:
   - **Production**: Port 9090 (check with `docker-compose up -d`)
   - **Development**: Port 50051 (check with `docker-compose -f docker-compose-dev.yml up -d`)
   
   **Fix**: Verify `application.properties` has the correct port:
   ```properties
   # For production (9090)
   data.service.url=localhost:9090
   
   # For development (50051)  
   data.service.url=localhost:50051
   ```

2. **Port mismatch between UI and data service**:
   - Symptom: UI shows "Connection refused" even though data service is running
   - Cause: UI configured for port 9090 but data service runs on 50051 (or vice versa)
   - **Fix**: Edit `application.properties` to match the data service port and rebuild JAR

3. **System property override not working**:
   - Symptom: `java -Ddata.service.url=localhost:50051 -jar ...` has no effect
   - Cause: UI reads **ONLY** from `application.properties`, not system properties
   - **Fix**: Edit `application.properties` directly and rebuild JAR

4. **No data displayed**: Verify database has answer events and user filter is set correctly
5. **UI freezes**: Data fetching uses `SwingWorker` to avoid blocking UI thread
6. **Missing protobuf classes**: Protobuf classes generated automatically during `mvn compile`
7. **Filter changes not updating data**: Ensure filter components are properly wired to data fetching logic

### Quick Diagnosis Steps

1. **Check data service status**:
   ```bash
   # For development
   curl -f http://localhost:8081/health/ready
   
   # For production  
   # (port 8081 should be available if data service is running)
   ```

2. **Verify UI configuration**:
   ```bash
   # Check what port the UI is configured to use
   grep "data.service.url" spaced-repetition-ui/src/main/resources/application.properties
   ```

3. **Check port mapping**:
   ```bash
   # See which ports are exposed
   docker ps --format "table {{.Names}}\t{{.Ports}}"
   ```

4. **Rebuild UI after configuration changes**:
   ```bash
   cd spaced-repetition-ui
   mvn clean package -DskipTests
   ```

### Debugging

Enable debug logging by setting system property:
```bash
java -Dlogging.level.org.company.spacedrepetition.ui=DEBUG ...
```

## Performance Considerations

- **Background fetching**: All data retrieval uses `SwingWorker` in background threads
- **Auto-refresh interval**: 5 minutes balances freshness with network usage
- **gRPC retry logic**: Exponential backoff (1s, 2s, 4s) for transient failures
- **Table performance**: Uses `DefaultTableModel` for efficient updates
- **Filter responsiveness**: Filter changes trigger immediate background updates

## Related Modules

- [Spaced Repetition Data Service](../spaced-repetition-data/README.md) - gRPC analytics service
- [Spaced Repetition Bot](../spaced-repetition-bot/README.md) - Primary Telegram bot
- [Spaced Repetition Protobuf](../spaced-repetition-protobuf/) - Protocol Buffer definitions

## Future Enhancements

1. **Chart visualizations**: Add bar charts, line graphs for quality distribution over time
2. **Export functionality**: Export data to CSV or PDF
3. **Multi-language support**: Internationalization
4. **Plugin architecture**: Allow custom visualization plugins
