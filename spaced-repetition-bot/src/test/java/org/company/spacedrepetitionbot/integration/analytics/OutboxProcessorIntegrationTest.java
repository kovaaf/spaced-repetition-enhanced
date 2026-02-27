package org.company.spacedrepetitionbot.integration.analytics;

import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.company.spacedrepetitionbot.service.analytics.OutboxProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for OutboxProcessor with PostgreSQL TestContainer and mock gRPC server.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OutboxProcessorIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static MockAnalyticsServer mockServer;

    static {
        try {
            mockServer = new MockAnalyticsServer();
            mockServer.start(0); // random port
        } catch (IOException e) {
            throw new RuntimeException("Failed to start mock gRPC server", e);
        }
    }

    @SpyBean
    private AnalyticsOutboxRepository analyticsOutboxRepository;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @AfterAll
    static void stopMockServer() throws InterruptedException {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
        // Override gRPC client address to point to mock server
        registry.add("grpc.client.analytics-service.address", 
                () -> "static://localhost:" + mockServer.getPort());
        // Disable Telegram bot and Git integration to avoid startup failures
        registry.add("telegram.bot.token", () -> "dummy-token");
        registry.add("telegram.bot.name", () -> "TestBot");
        registry.add("app.default-deck.repo.url", () -> "");
        registry.add("app.git-sync.enabled", () -> "false");
        registry.add("app.analytics.outbox.processor.cron", () -> "0 0 0 * * *"); // disable frequent runs
    }

    @BeforeEach
    void setUp() {
        analyticsOutboxRepository.deleteAll();
        mockServer.clearReceivedEvents();
        mockServer.setShouldFail(false);
        mockServer.setFailureException(null);
        Mockito.reset(analyticsOutboxRepository);
    }

    @Test
    void processOutbox_happyPath_shouldSendEventToGrpcServer() throws InterruptedException {
        // Given: a pending analytics outbox record
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(123L)
                .deckId(456L)
                .cardId(789L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        analyticsOutboxRepository.save(record);

        // When: outbox processor runs
        outboxProcessor.processOutbox();

        // Then: record status should be COMPLETED
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(record.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.COMPLETED, updated.getStatus());
        assertNotNull(updated.getProcessedAt());
        assertNull(updated.getErrorMessage());

        // And: mock server should have received the event
        List<org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent> receivedEvents = mockServer.getReceivedEvents();
        assertEquals(1, receivedEvents.size());
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent event = receivedEvents.get(0);
        assertEquals("123", event.getUserId());
        assertEquals("456", event.getDeckId());
        assertEquals("789", event.getCardId());
        assertEquals(org.company.spacedrepetitiondata.grpc.AnalyticsProto.Quality.GOOD, event.getQuality());
    }

    @Test
    void processOutbox_grpcServerError_shouldRetryAndUpdateStatus() throws InterruptedException {
        // Given: a pending record and mock server set to fail
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(123L)
                .deckId(456L)
                .cardId(789L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        analyticsOutboxRepository.save(record);
        mockServer.setShouldFail(true);
        mockServer.setFailureException(new RuntimeException("Service unavailable"));

        // When: outbox processor runs
        outboxProcessor.processOutbox();

        // Then: record status should be FAILED, retry count incremented, nextRetryAt set
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(record.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, updated.getStatus());
        assertEquals(1, updated.getRetryCount());
        assertNotNull(updated.getNextRetryAt());
        assertNotNull(updated.getLastRetryAt());
        assertNotNull(updated.getErrorMessage());
        assertTrue(updated.getErrorMessage().contains("Service unavailable"));
    }

    @Test
    void processOutbox_maxRetriesExceeded_shouldMoveToDlq() throws InterruptedException {
        // Given: a pending record with retry count already at max (5) and server failing
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(123L)
                .deckId(456L)
                .cardId(789L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(5) // max retries
                .build();
        analyticsOutboxRepository.save(record);
        mockServer.setShouldFail(true);

        // When: outbox processor runs
        outboxProcessor.processOutbox();

        // Then: record status should be DLQ, retry count incremented
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(record.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.DLQ, updated.getStatus());
        assertEquals(6, updated.getRetryCount());
        assertNotNull(updated.getErrorMessage());
    }

    @Test
    void processOutbox_transactionalRollback_shouldNotUpdateStatusOnUnexpectedError() {
        // Given: a pending analytics outbox record
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(123L)
                .deckId(456L)
                .cardId(789L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        analyticsOutboxRepository.save(record);
        
        // And: repository save will throw RuntimeException on any save attempt
        Mockito.doThrow(new RuntimeException("Database constraint violation"))
                .when(analyticsOutboxRepository)
                .save(ArgumentMatchers.any(AnalyticsOutbox.class));
        
        // When & Then: outbox processor should throw exception due to save failure
        assertThrows(RuntimeException.class, () -> outboxProcessor.processOutbox());
        
        // And: record status should remain PENDING (transaction rolled back)
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(record.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.PENDING, updated.getStatus());
        assertEquals(0, updated.getRetryCount());
        assertNull(updated.getNextRetryAt());
        assertNull(updated.getLastRetryAt());
        assertNull(updated.getErrorMessage());
    }


}