package org.company.spacedrepetitionbot.repository.analytics;

import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data JPA test for AnalyticsOutbox entity persistence using H2 embedded database.
 * Tests JPA repository operations with sequence-generated IDs.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.default_schema=public",
    "spring.jpa.show-sql=false",
    "spring.liquibase.enabled=false"
})
class AnalyticsOutboxRepositoryDataJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnalyticsOutboxRepository analyticsOutboxRepository;

    @BeforeEach
    void setUp() {
        analyticsOutboxRepository.deleteAll();
    }

    @Test
    void save_shouldGenerateSequenceId() {
        // Given
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(123L)
                .deckId(456L)
                .cardId(789L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        // When
        AnalyticsOutbox saved = analyticsOutboxRepository.save(record);

        // Then
        assertNotNull(saved.getEventId());
        assertTrue(saved.getEventId() > 0L, "Event ID should be positive");
        assertEquals(record.getUserId(), saved.getUserId());
        assertEquals(record.getDeckId(), saved.getDeckId());
        assertEquals(record.getCardId(), saved.getCardId());
    }

    @Test
    void findById_shouldReturnSavedEntity() {
        // Given
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(1L)
                .deckId(2L)
                .cardId(3L)
                .quality(Quality.EASY.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox saved = analyticsOutboxRepository.save(record);
        Long eventId = saved.getEventId();

        // When
        Optional<AnalyticsOutbox> found = analyticsOutboxRepository.findById(eventId);

        // Then
        assertTrue(found.isPresent());
        AnalyticsOutbox retrieved = found.get();
        assertEquals(eventId, retrieved.getEventId());
        assertEquals(saved.getUserId(), retrieved.getUserId());
        assertEquals(saved.getDeckId(), retrieved.getDeckId());
        assertEquals(saved.getCardId(), retrieved.getCardId());
    }

    @Test
    void save_multipleEntities_shouldGenerateSequentialIds() {
        // Given
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

        // When
        AnalyticsOutbox saved1 = analyticsOutboxRepository.save(record1);
        AnalyticsOutbox saved2 = analyticsOutboxRepository.save(record2);

        // Then
        assertNotNull(saved1.getEventId());
        assertNotNull(saved2.getEventId());
        assertTrue(saved1.getEventId() < saved2.getEventId(), "IDs should be increasing");
        assertEquals(saved1.getEventId() + 1, saved2.getEventId(), "IDs should be sequential");
    }

    @Test
    @Transactional
    void update_shouldModifyExistingEntity() {
        // Given
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(100L)
                .deckId(200L)
                .cardId(300L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox saved = analyticsOutboxRepository.save(record);
        Long eventId = saved.getEventId();

        // When
        saved.setStatus(OutboxStatus.COMPLETED);
        saved.setRetryCount(5);
        analyticsOutboxRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // Then
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(eventId).orElseThrow();
        assertEquals(OutboxStatus.COMPLETED, updated.getStatus());
        assertEquals(5, updated.getRetryCount());
        assertNotNull(updated.getProcessedAt()); // Should be set by @PreUpdate
    }

    @Test
    void delete_shouldRemoveEntity() {
        // Given
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(999L)
                .deckId(999L)
                .cardId(999L)
                .quality(Quality.HARD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox saved = analyticsOutboxRepository.save(record);
        Long eventId = saved.getEventId();

        // When
        analyticsOutboxRepository.deleteById(eventId);

        // Then
        Optional<AnalyticsOutbox> deleted = analyticsOutboxRepository.findById(eventId);
        assertFalse(deleted.isPresent());
    }

    @Test
    @Transactional
    void transactionRollback_shouldNotPersistChanges() {
        // Given
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(500L)
                .deckId(500L)
                .cardId(500L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox saved = analyticsOutboxRepository.save(record);
        Long eventId = saved.getEventId();
        // Commit the initial save
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Load the entity in the new transaction
        AnalyticsOutbox toModify = analyticsOutboxRepository.findById(eventId).orElseThrow();

        // Simulate a runtime exception after modifying entity within new transaction
        try {
            toModify.setStatus(OutboxStatus.FAILED);
            analyticsOutboxRepository.save(toModify);
            // Simulate an error that triggers rollback
            throw new RuntimeException("Simulated error");
        } catch (RuntimeException e) {
            // Expected - transaction should rollback
        }
        // Then - entity should still have original status (transaction rolled back)
        // Since the second transaction rolled back, modifications should not persist
        // However, the initial save was committed, so entity should exist with original status
        entityManager.clear();
        AnalyticsOutbox afterRollback = analyticsOutboxRepository.findById(eventId).orElseThrow();
        assertEquals(OutboxStatus.PENDING, afterRollback.getStatus());
    }

    @Test
    void findByStatus_shouldReturnMatchingRecords() {
        // Given
        AnalyticsOutbox pending1 = AnalyticsOutbox.builder()
                .userId(1L)
                .deckId(1L)
                .cardId(1L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox pending2 = AnalyticsOutbox.builder()
                .userId(2L)
                .deckId(2L)
                .cardId(2L)
                .quality(Quality.EASY.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        AnalyticsOutbox completed = AnalyticsOutbox.builder()
                .userId(3L)
                .deckId(3L)
                .cardId(3L)
                .quality(Quality.HARD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.COMPLETED)
                .retryCount(0)
                .build();
        analyticsOutboxRepository.saveAll(List.of(pending1, pending2, completed));

        // When
        List<AnalyticsOutbox> pendingRecords = analyticsOutboxRepository.findByStatus(OutboxStatus.PENDING);

        // Then
        assertEquals(2, pendingRecords.size());
        assertTrue(pendingRecords.stream().allMatch(r -> r.getStatus() == OutboxStatus.PENDING));
    }
}