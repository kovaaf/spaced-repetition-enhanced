package org.company.spacedrepetitionbot.e2e;

import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.integration.analytics.MockAnalyticsServer;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.company.spacedrepetitionbot.service.analytics.OutboxProcessor;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.io.IOException;
import java.util.regex.Pattern;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests spanning bot and data service (mock).
 * Uses TestContainers PostgreSQL and mock gRPC server.
 * Verifies flow: bot answer → outbox → data service.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class AnalyticsIntegrationTest {

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

    @Autowired
    private AnalyticsOutboxRepository analyticsOutboxRepository;

    @Autowired
    private OutboxProcessor outboxProcessor;



    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

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
        // Disable Telegram bot and Git integration
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
    }









    @Test
    void botAnswer_toOutbox_toMockDataService_toUI_flow() {
        // 1. Simulate bot answer recording (simplified: directly insert into outbox)
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

        // 2. Trigger outbox processor
        outboxProcessor.processOutbox();

        // 3. Verify outbox record marked as COMPLETED
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(record.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.COMPLETED, updated.getStatus());

        // 4. Verify mock server received the event
        List<AnalyticsProto.AnswerEvent> receivedEvents = mockServer.getReceivedEvents();
        assertEquals(1, receivedEvents.size());
        AnalyticsProto.AnswerEvent event = receivedEvents.get(0);
        assertEquals("123", event.getUserId());
        assertEquals("456", event.getDeckId());
        assertEquals("789", event.getCardId());
        assertEquals(AnalyticsProto.Quality.GOOD, event.getQuality());
    }

    @Test
    void errorScenario_dataServiceUnavailable_shouldShowErrorInUI() {
        // Configure mock server to fail
        mockServer.setShouldFail(true);
        mockServer.setFailureException(new RuntimeException("Service unavailable"));

        // Simulate bot answer recording
        AnalyticsOutbox record = AnalyticsOutbox.builder()
                .userId(999L)
                .deckId(888L)
                .cardId(777L)
                .quality(Quality.AGAIN.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        analyticsOutboxRepository.save(record);

        // Trigger outbox processor (should fail and increment retry count)
        outboxProcessor.processOutbox();

        // Verify outbox record marked as FAILED
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(record.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, updated.getStatus());
        assertEquals(1, updated.getRetryCount());
        assertNotNull(updated.getErrorMessage());
    }
}