package org.company.spacedrepetitiondata.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AnswerEventRepository using TestContainers PostgreSQL.
 * Creates the answer_events table and related foreign key tables (user_info, deck, card) as minimal stubs.
 */
@Testcontainers
@Disabled("TestContainers Docker environment issue - see issues.md")
@ExtendWith({})
class AnswerEventRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17.5-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static DataSource testDataSource;
    private AnswerEventRepository repository;

    @BeforeAll
    static void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");
        testDataSource = new HikariDataSource(config);
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Create required tables (simplified stubs for foreign keys)
        try (Connection conn = testDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Create user_info stub (only needed column)
            stmt.execute("CREATE TABLE IF NOT EXISTS user_info (" +
                    "user_chat_id BIGINT PRIMARY KEY" +
                    ")");
            // Create deck stub
            stmt.execute("CREATE TABLE IF NOT EXISTS deck (" +
                    "deck_id BIGINT PRIMARY KEY" +
                    ")");
            // Create card stub
            stmt.execute("CREATE TABLE IF NOT EXISTS card (" +
                    "card_id BIGINT PRIMARY KEY" +
                    ")");
            // Create answer_events table (matching migration)
            stmt.execute("CREATE TABLE IF NOT EXISTS answer_events (" +
                    "event_id BIGSERIAL PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "deck_id BIGINT NOT NULL," +
                    "card_id BIGINT NOT NULL," +
                    "quality INTEGER NOT NULL," +
                    "event_timestamp TIMESTAMP NOT NULL DEFAULT now()," +
                    "created_at TIMESTAMP NOT NULL DEFAULT now()," +
                    "outbox_id BIGINT," +
                    "CONSTRAINT ck_answer_events_quality CHECK (quality IN (0, 3, 4, 5))," +
                    "CONSTRAINT fk_answer_events_user FOREIGN KEY (user_id) REFERENCES user_info(user_chat_id)," +
                    "CONSTRAINT fk_answer_events_deck FOREIGN KEY (deck_id) REFERENCES deck(deck_id)," +
                    "CONSTRAINT fk_answer_events_card FOREIGN KEY (card_id) REFERENCES card(card_id)" +
                    ")");
            // Create indexes (optional for tests)
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_timestamp ON answer_events(user_id, event_timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON answer_events(event_timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_quality ON answer_events(user_id, quality)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_answer_events_outbox ON answer_events(outbox_id)");
        }
        // Insert stub foreign key rows to satisfy constraints
        try (Connection conn = testDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO user_info (user_chat_id) VALUES (1001) ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO deck (deck_id) VALUES (2001) ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO card (card_id) VALUES (3001) ON CONFLICT DO NOTHING");
        }
        repository = new AnswerEventRepository(testDataSource);
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Truncate answer_events table after each test
        try (Connection conn = testDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE answer_events CASCADE");
        }
    }

    @Test
    void insert_shouldPersistAnswerEventAndReturnGeneratedId() {
        AnswerEventRecord record = new AnswerEventRecord(1001L, 2001L, 3001L, 4, Instant.now());
        Optional<Long> eventId = repository.insert(record);
        assertTrue(eventId.isPresent());
        assertTrue(eventId.get() > 0);
    }

    @Test
    void insert_shouldRejectInvalidQuality() {
        AnswerEventRecord record = new AnswerEventRecord(1001L, 2001L, 3001L, 99, Instant.now());
        // IllegalArgumentException from AnswerEventRecord constructor
        assertThrows(IllegalArgumentException.class, () -> new AnswerEventRecord(1001L, 2001L, 3001L, 99, Instant.now()));
    }

    @Test
    void findByUserIdAndTimeRange_shouldReturnEventsWithinRange() {
        Instant now = Instant.now();
        AnswerEventRecord event1 = new AnswerEventRecord(1001L, 2001L, 3001L, 4, now.minusSeconds(300));
        AnswerEventRecord event2 = new AnswerEventRecord(1001L, 2001L, 3001L, 5, now.minusSeconds(200));
        AnswerEventRecord event3 = new AnswerEventRecord(1001L, 2001L, 3001L, 3, now.minusSeconds(100));
        repository.insert(event1);
        repository.insert(event2);
        repository.insert(event3);

        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(1001L,
                now.minusSeconds(250), now.minusSeconds(50));
        assertEquals(1, results.size());
        assertEquals(event2.getQuality(), results.get(0).getQuality());
        assertEquals(event2.getTimestamp().toEpochMilli(), results.get(0).getTimestamp().toEpochMilli());
    }

    @Test
    void findByUserIdAndTimeRange_shouldReturnAllEventsWhenNoBounds() {
        Instant now = Instant.now();
        AnswerEventRecord event1 = new AnswerEventRecord(1001L, 2001L, 3001L, 4, now.minusSeconds(300));
        AnswerEventRecord event2 = new AnswerEventRecord(1001L, 2001L, 3001L, 5, now.minusSeconds(200));
        repository.insert(event1);
        repository.insert(event2);

        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(1001L, null, null);
        assertEquals(2, results.size());
        // Should be ordered by timestamp ascending
        assertTrue(results.get(0).getTimestamp().isBefore(results.get(1).getTimestamp()));
    }

    @Test
    void findByUserIdAndTimeRange_shouldReturnEmptyListForNonExistentUser() {
        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(9999L, null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void countByUserId_shouldReturnCorrectCount() {
        Instant now = Instant.now();
        repository.insert(new AnswerEventRecord(1001L, 2001L, 3001L, 4, now.minusSeconds(300)));
        repository.insert(new AnswerEventRecord(1001L, 2001L, 3001L, 5, now.minusSeconds(200)));
        repository.insert(new AnswerEventRecord(2002L, 2001L, 3001L, 3, now.minusSeconds(100)));

        long count = repository.countByUserId(1001L);
        assertEquals(2, count);
        count = repository.countByUserId(2002L);
        assertEquals(1, count);
        count = repository.countByUserId(9999L);
        assertEquals(0, count);
    }

    @Test
    void fromProto_and_toProto_shouldConvertCorrectly() {
        // Create a protobuf AnswerEvent (string IDs)
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent protoEvent =
                org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("1001")
                        .setDeckId("2001")
                        .setCardId("3001")
                        .setQualityValue(4)
                        .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(1234567890L)
                                .setNanos(123456789)
                                .build())
                        .build();

        AnswerEventRecord record = AnswerEventRepository.fromProto(protoEvent);
        assertEquals(1001L, record.getUserId());
        assertEquals(2001L, record.getDeckId());
        assertEquals(3001L, record.getCardId());
        assertEquals(4, record.getQuality());
        assertEquals(1234567890L, record.getTimestamp().getEpochSecond());
        assertEquals(123456789, record.getTimestamp().getNano());

        // Convert back
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent convertedProto =
                AnswerEventRepository.toProto(record);
        assertEquals("1001", convertedProto.getUserId());
        assertEquals("2001", convertedProto.getDeckId());
        assertEquals("3001", convertedProto.getCardId());
        assertEquals(4, convertedProto.getQualityValue());
        assertEquals(1234567890L, convertedProto.getTimestamp().getSeconds());
        assertEquals(123456789, convertedProto.getTimestamp().getNanos());
    }
}