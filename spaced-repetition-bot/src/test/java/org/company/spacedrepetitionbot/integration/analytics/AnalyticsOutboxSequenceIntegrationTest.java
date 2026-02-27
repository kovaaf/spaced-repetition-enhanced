package org.company.spacedrepetitionbot.integration.analytics;

import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AnalyticsOutbox sequence functionality.
 * Tests sequence creation, ID generation, entity persistence, and rollback scenarios.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AnalyticsOutboxSequenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AnalyticsOutboxRepository analyticsOutboxRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
        // Disable Telegram bot and Git integration to avoid startup failures
        registry.add("telegram.bot.token", () -> "dummy-token");
        registry.add("telegram.bot.name", () -> "TestBot");
        registry.add("app.default-deck.repo.url", () -> "");
        registry.add("app.git-sync.enabled", () -> "false");
        registry.add("app.analytics.outbox.processor.cron", () -> "0 0 0 * * *"); // disable frequent runs
        // Set dummy gRPC address to prevent connection attempts
        registry.add("grpc.client.analytics-service.address", () -> "static://localhost:9099");
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null) {
            postgres.close();
        }
    }

    @BeforeEach
    void setUp() {
        analyticsOutboxRepository.deleteAll();
    }

    @Test
    void sequence_analytics_outbox_seq_should_exist_and_generate_numbers() {
        // Verify that the sequence analytics_outbox_seq exists
        List<?> result = entityManager.createNativeQuery(
                "SELECT nextval('analytics_outbox_seq')"
        ).getResultList();
        assertNotNull(result);
        // First call to nextval should return a number (starting from 1 or current value)
        Long firstValue = ((Number) result.get(0)).longValue();
        assertTrue(firstValue > 0, "Sequence should generate positive numbers");

        // Call nextval again to verify increment
        Long secondValue = ((Number) entityManager.createNativeQuery(
                "SELECT nextval('analytics_outbox_seq')"
        ).getSingleResult()).longValue();
        assertEquals(firstValue + 1, secondValue, "Sequence should increment by 1");
    }

    @Test
    @Transactional
    void entity_persistence_should_use_sequence_for_id_generation() {
        // Given: a new AnalyticsOutbox entity
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(123L)
                .deckId(456L)
                .cardId(789L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        // When: entity is persisted
        AnalyticsOutbox saved = analyticsOutboxRepository.save(record);

        // Then: eventId should be generated from sequence (non-null, positive)
        assertNotNull(saved.getEventId());
        assertTrue(saved.getEventId() > 0L, "Event ID should be positive");

        // And: entity can be retrieved by that ID
        AnalyticsOutbox retrieved = analyticsOutboxRepository.findById(saved.getEventId())
                .orElseThrow(() -> new AssertionError("Saved entity should be retrievable"));
        assertEquals(saved.getEventId(), retrieved.getEventId());
        assertEquals(saved.getUserId(), retrieved.getUserId());
        assertEquals(saved.getDeckId(), retrieved.getDeckId());
        assertEquals(saved.getCardId(), retrieved.getCardId());
    }

    @Test
    @Transactional
    void multiple_entities_should_get_sequential_ids() {
        // Given: three new AnalyticsOutbox entities
        AnalyticsOutbox record1 = AnalyticsOutbox.builder()
                .userId(1L)
                .deckId(1L)
                .cardId(1L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox record2 = AnalyticsOutbox.builder()
                .userId(2L)
                .deckId(2L)
                .cardId(2L)
                .quality(Quality.EASY.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox record3 = AnalyticsOutbox.builder()
                .userId(3L)
                .deckId(3L)
                .cardId(3L)
                .quality(Quality.HARD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        // When: entities are persisted in transaction
        AnalyticsOutbox saved1 = analyticsOutboxRepository.save(record1);
        AnalyticsOutbox saved2 = analyticsOutboxRepository.save(record2);
        AnalyticsOutbox saved3 = analyticsOutboxRepository.save(record3);

        // Then: IDs should be sequential and increasing
        assertNotNull(saved1.getEventId());
        assertNotNull(saved2.getEventId());
        assertNotNull(saved3.getEventId());
        assertTrue(saved1.getEventId() < saved2.getEventId(), "IDs should be increasing");
        assertTrue(saved2.getEventId() < saved3.getEventId(), "IDs should be increasing");
        assertEquals(saved1.getEventId() + 1, saved2.getEventId(), "IDs should be sequential");
        assertEquals(saved2.getEventId() + 1, saved3.getEventId(), "IDs should be sequential");
    }

    @Test
    @Transactional
    void rollback_scenario_should_not_consume_sequence_values() {
        // Get current sequence value before attempted failed transaction
        Long beforeValue = ((Number) entityManager.createNativeQuery(
                "SELECT last_value FROM analytics_outbox_seq"
        ).getSingleResult()).longValue();

        try {
            // Start a transaction that will fail due to constraint violation
            // Create entity with null required fields to trigger constraint violation
            AnalyticsOutbox invalidRecord = AnalyticsOutbox.builder()
                    .userId(null) // This will cause constraint violation
                    .deckId(456L)
                    .cardId(789L)
                    .quality(Quality.GOOD.getQuality())
                    .eventTimestamp(LocalDateTime.now())
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            analyticsOutboxRepository.save(invalidRecord);
            entityManager.flush(); // Force flush to trigger constraint violation
            fail("Expected constraint violation exception");
        } catch (Exception e) {
            // Expected - transaction should roll back
            assertTrue(e instanceof jakarta.persistence.PersistenceException ||
                       e.getCause() instanceof jakarta.persistence.PersistenceException ||
                       e instanceof org.springframework.dao.DataIntegrityViolationException,
                       "Should throw persistence or data integrity exception");
        }

        // After rollback, sequence value should not have advanced
        Long afterValue = ((Number) entityManager.createNativeQuery(
                "SELECT last_value FROM analytics_outbox_seq"
        ).getSingleResult()).longValue();
        assertEquals(beforeValue, afterValue, "Sequence last_value should not change after rollback");

        // Verify we can still insert a valid record after rollback
        AnalyticsOutbox validRecord = AnalyticsOutbox.builder()
                .userId(999L)
                .deckId(999L)
                .cardId(999L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox saved = analyticsOutboxRepository.save(validRecord);
        assertNotNull(saved.getEventId());
        // The new ID should be the next sequence value after beforeValue
        assertTrue(saved.getEventId() > beforeValue, "New record should get next sequence value");
    }
}