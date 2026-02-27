package org.company.spacedrepetitionbot.e2e;

import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.integration.analytics.MockAnalyticsServer;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsDLQ;
import org.company.spacedrepetitionbot.repository.CardRepository;
import org.company.spacedrepetitionbot.repository.DeckRepository;
import org.company.spacedrepetitionbot.repository.LearningSessionRepository;
import org.company.spacedrepetitionbot.repository.UserInfoRepository;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsDLQRepository;
import org.company.spacedrepetitionbot.service.analytics.OutboxProcessor;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;

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
import java.time.LocalDateTime;
import java.util.List;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.learn.answer.AnswerGoodStrategy;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for analytics event flow from card answer through outbox to gRPC data service.
 * Simulates the complete pipeline: card answer → analytics outbox creation → outbox processing → gRPC transmission.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class AnalyticsEventFlowEndToEndTest {

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
    private UserInfoRepository userInfoRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    @Autowired
    private LearningSessionService learningSessionService;

    @Autowired
    private AnalyticsOutboxRepository analyticsOutboxRepository;
    @Autowired
    private AnalyticsDLQRepository analyticsDLQRepository;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @MockBean
    private TelegramClient telegramClient;

    @MockBean
    private MessageStateService messageStateService;

    @MockBean
    private MarkdownEscaper markdownEscaper;

    @MockBean
    private KeyboardManager keyboardManager;
    @Autowired
    private AnswerGoodStrategy answerGoodStrategy;
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
        learningSessionRepository.deleteAll();
        cardRepository.deleteAll();
        deckRepository.deleteAll();
        userInfoRepository.deleteAll();
        mockServer.clearReceivedEvents();
        mockServer.setShouldFail(false);
        mockServer.setFailureException(null);
    }

    /**
     * Helper method to create a test user.
     */
    private UserInfo createTestUser(Long chatId) {
        UserInfo user = UserInfo.builder()
                .userChatId(chatId)
                .userName("testuser")
                .build();
        return userInfoRepository.save(user);
    }

    /**
     * Helper method to create a test deck owned by the given user.
     */
    private Deck createTestDeck(UserInfo owner, String name) {
        Deck deck = Deck.builder()
                .owner(owner)
                .name(name)
                .build();
        return deckRepository.save(deck);
    }

    /**
     * Helper method to create a test card in the given deck.
     */
    private Card createTestCard(Deck deck, String front, String back) {
        Card card = Card.builder()
                .deck(deck)
                .front(front)
                .back(back)
                .status(Status.NEW)
                .repeatCount(0)
                .easinessFactor(2.5)
                .nextReviewTime(LocalDateTime.now())
                .build();
        return cardRepository.save(card);
    }

    /**
     * Helper method to create a learning session for the given deck.
     */
    private LearningSession createLearningSession(Deck deck) {
        LearningSession session = LearningSession.builder()
                .deck(deck)
                .createdAt(LocalDateTime.now())
                .build();
        return learningSessionRepository.save(session);
    }

    /**
     * Helper method to add a card to a learning session.
     */
    private void addCardToSession(LearningSession session, Card card) {
        session.getCards().add(card);
        learningSessionRepository.save(session);
    }


    @Test
    void cardAnswer_shouldCreateAnalyticsOutboxAndSendToGrpc() {
        // Given
        Long chatId = 12345L;
        UserInfo user = createTestUser(chatId);
        Deck deck = createTestDeck(user, "Test Deck");
        Card card = createTestCard(deck, "What is Java?", "A programming language");
        LearningSession session = createLearningSession(deck);
        addCardToSession(session, card);

        // Mock keyboard manager and markdown escaper to avoid NPE
        when(keyboardManager.getLearnDeckKeyboard(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(null);
        when(markdownEscaper.escapeMarkdownV2(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Using autowired AnswerGoodStrategy bean with mocked dependencies

        // Build callback query with correct data format
        String callbackData = "GOOD:=" + card.getCardId() + ":=" + deck.getDeckId() + ":=" + session.getSessionId();
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);

        // When: simulate card answer
        answerGoodStrategy.executeCallbackQuery(callbackQuery);

        // Then: analytics outbox record should be created
        List<AnalyticsOutbox> outboxRecords = analyticsOutboxRepository.findAll();
        assertEquals(1, outboxRecords.size());
        AnalyticsOutbox outbox = outboxRecords.get(0);
        assertEquals(user.getUserChatId(), outbox.getUserId());
        assertEquals(deck.getDeckId(), outbox.getDeckId());
        assertEquals(card.getCardId(), outbox.getCardId());
        assertEquals(Quality.GOOD.getQuality(), outbox.getQuality());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());

        // When: trigger outbox processor
        outboxProcessor.processOutbox();

        // Then: outbox record should be marked as COMPLETED
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(outbox.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.COMPLETED, updated.getStatus());
        assertNotNull(updated.getProcessedAt());

        // And: mock server should have received the event
        List<org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent> receivedEvents = mockServer.getReceivedEvents();
        assertEquals(1, receivedEvents.size());
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent event = receivedEvents.get(0);
        assertEquals(String.valueOf(user.getUserChatId()), event.getUserId());
        assertEquals(String.valueOf(deck.getDeckId()), event.getDeckId());
        assertEquals(String.valueOf(card.getCardId()), event.getCardId());
        assertEquals(org.company.spacedrepetitiondata.grpc.AnalyticsProto.Quality.GOOD, event.getQuality());
    }

    @Test
    void cardAnswer_grpcFailure_shouldIncrementRetryAndSetFailedStatus() {
        // Given
        Long chatId = 12345L;
        UserInfo user = createTestUser(chatId);
        Deck deck = createTestDeck(user, "Test Deck");
        Card card = createTestCard(deck, "What is Java?", "A programming language");
        LearningSession session = createLearningSession(deck);
        addCardToSession(session, card);

        // Mock keyboard manager and markdown escaper to avoid NPE
        when(keyboardManager.getLearnDeckKeyboard(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(null);
        when(markdownEscaper.escapeMarkdownV2(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Build callback query with correct data format
        String callbackData = "GOOD:=" + card.getCardId() + ":=" + deck.getDeckId() + ":=" + session.getSessionId();
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);

        // When: simulate card answer
        answerGoodStrategy.executeCallbackQuery(callbackQuery);

        // Then: analytics outbox record should be created
        List<AnalyticsOutbox> outboxRecords = analyticsOutboxRepository.findAll();
        assertEquals(1, outboxRecords.size());
        AnalyticsOutbox outbox = outboxRecords.get(0);
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());

        // And: mock server set to fail
        mockServer.setShouldFail(true);
        mockServer.setFailureException(new RuntimeException("Service unavailable"));

        // When: trigger outbox processor
        outboxProcessor.processOutbox();

        // Then: record status should be FAILED, retry count incremented, nextRetryAt set
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(outbox.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, updated.getStatus());
        assertEquals(1, updated.getRetryCount());
        assertNotNull(updated.getNextRetryAt());
        assertNotNull(updated.getLastRetryAt());
        assertNotNull(updated.getErrorMessage());
        assertTrue(updated.getErrorMessage().contains("Service unavailable"));
        // Mock server should NOT have received the event
        List<org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent> receivedEvents = mockServer.getReceivedEvents();
        assertEquals(0, receivedEvents.size());
    }

    @Test
    void cardAnswer_maxRetriesExceeded_shouldMoveToDLQ() {
        // Given
        Long chatId = 12345L;
        UserInfo user = createTestUser(chatId);
        Deck deck = createTestDeck(user, "Test Deck");
        Card card = createTestCard(deck, "What is Java?", "A programming language");
        LearningSession session = createLearningSession(deck);
        addCardToSession(session, card);

        // Mock keyboard manager and markdown escaper to avoid NPE
        when(keyboardManager.getLearnDeckKeyboard(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(null);
        when(markdownEscaper.escapeMarkdownV2(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Build callback query with correct data format
        String callbackData = "GOOD:=" + card.getCardId() + ":=" + deck.getDeckId() + ":=" + session.getSessionId();
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);

        // When: simulate card answer
        answerGoodStrategy.executeCallbackQuery(callbackQuery);

        // Then: analytics outbox record should be created
        List<AnalyticsOutbox> outboxRecords = analyticsOutboxRepository.findAll();
        assertEquals(1, outboxRecords.size());
        AnalyticsOutbox outbox = outboxRecords.get(0);
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());

        // Manually set retry count to 5 (max retries) and status to FAILED to simulate exhausted retries
        outbox.setRetryCount(5);
        outbox.setStatus(OutboxStatus.FAILED);
        analyticsOutboxRepository.save(outbox);

        // And: mock server set to fail
        mockServer.setShouldFail(true);

        // When: trigger outbox processor
        outboxProcessor.processOutbox();

        // Then: record status should be DLQ, retry count incremented to 6
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(outbox.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.DLQ, updated.getStatus());
        assertEquals(6, updated.getRetryCount());
        assertNotNull(updated.getErrorMessage());
        // Mock server should NOT have received the event
        List<org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent> receivedEvents = mockServer.getReceivedEvents();
        assertEquals(0, receivedEvents.size());
    }

    @Test
    void cardAnswer_dlqRecord_shouldAppearInDLQRepository() {
        // Given
        Long chatId = 12345L;
        UserInfo user = createTestUser(chatId);
        Deck deck = createTestDeck(user, "Test Deck");
        Card card = createTestCard(deck, "What is Java?", "A programming language");
        LearningSession session = createLearningSession(deck);
        addCardToSession(session, card);

        // Mock keyboard manager and markdown escaper to avoid NPE
        when(keyboardManager.getLearnDeckKeyboard(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(null);
        when(markdownEscaper.escapeMarkdownV2(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Build callback query with correct data format
        String callbackData = "GOOD:=" + card.getCardId() + ":=" + deck.getDeckId() + ":=" + session.getSessionId();
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);

        // When: simulate card answer
        answerGoodStrategy.executeCallbackQuery(callbackQuery);

        // Then: analytics outbox record should be created
        List<AnalyticsOutbox> outboxRecords = analyticsOutboxRepository.findAll();
        assertEquals(1, outboxRecords.size());
        AnalyticsOutbox outbox = outboxRecords.get(0);
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());

        // Manually set retry count to 5 (max retries exceeded) and status to FAILED
        outbox.setRetryCount(5);
        outbox.setStatus(OutboxStatus.FAILED);
        analyticsOutboxRepository.save(outbox);

        // And: mock server set to fail
        mockServer.setShouldFail(true);

        // When: trigger outbox processor (should move to DLQ)
        outboxProcessor.processOutbox();

        // Then: outbox record status should be DLQ
        AnalyticsOutbox updated = analyticsOutboxRepository.findById(outbox.getEventId()).orElseThrow();
        assertEquals(OutboxStatus.DLQ, updated.getStatus());

        // And: DLQ repository should contain the record
        List<AnalyticsDLQ> dlqRecords = analyticsDLQRepository.findAll();
        assertEquals(1, dlqRecords.size());
        AnalyticsDLQ dlq = dlqRecords.get(0);
        assertEquals(outbox.getEventId(), dlq.getOutboxId());
        assertEquals(outbox.getUserId(), dlq.getUserId());
        assertEquals(outbox.getDeckId(), dlq.getDeckId());
        assertEquals(outbox.getCardId(), dlq.getCardId());
        assertEquals(outbox.getQuality(), dlq.getQuality());
        assertEquals(outbox.getEventTimestamp(), dlq.getEventTimestamp());
        assertEquals(outbox.getRetryCount(), dlq.getRetryCount());
        assertNotNull(dlq.getFailureReason());
}
}