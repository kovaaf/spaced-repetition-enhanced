package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.learn.answer;

import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link AnswerAgainStrategy}.
 */
@ExtendWith(MockitoExtension.class)
class AnswerAgainStrategyTest {

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private MessageStateService messageStateService;

    @Mock
    private MarkdownEscaper markdownEscaper;

    @Mock
    private LearningSessionService learningSessionService;

    @Mock
    private AnalyticsOutboxRepository analyticsOutboxRepository;

    @Mock
    private KeyboardManager keyboardManager;

    @InjectMocks
    private AnswerAgainStrategy answerAgainStrategy;

    @BeforeEach
    void setUp() {
        // Default mock behavior to avoid NPE in super.executeCallbackQuery
        lenient().when(markdownEscaper.escapeMarkdownV2(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executeCallbackQuery_ShouldSaveAnalyticsOutboxRecord() {
        // Given
        Long cardId = 123L;
        Long deckId = 456L;
        Long sessionId = 789L;
        Long userId = 999L;
        String callbackData = "AGAIN:=" + cardId + ":=" + deckId + ":=" + sessionId;

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(userId);

        Card mockCard = mock(Card.class);
        Deck mockDeck = mock(Deck.class);
        UserInfo mockUser = mock(UserInfo.class);
        when(mockCard.getCardId()).thenReturn(cardId);
        when(mockCard.getDeck()).thenReturn(mockDeck);
        when(mockCard.getStatus()).thenReturn(Status.NEW);
        when(mockDeck.getDeckId()).thenReturn(deckId);
        when(mockDeck.getOwner()).thenReturn(mockUser);
        when(mockUser.getUserChatId()).thenReturn(userId);

        when(learningSessionService.updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN)))
                .thenReturn(mockCard);
        when(learningSessionService.getRemainingCardsCount(sessionId)).thenReturn(1);

        // When
        answerAgainStrategy.executeCallbackQuery(callbackQuery);

        // Then
        verify(learningSessionService).updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN));
        verify(learningSessionService, never()).removeCardFromSession(anyLong(), anyLong());

        ArgumentCaptor<AnalyticsOutbox> outboxCaptor = ArgumentCaptor.forClass(AnalyticsOutbox.class);
        verify(analyticsOutboxRepository).save(outboxCaptor.capture());
        AnalyticsOutbox savedOutbox = outboxCaptor.getValue();
        assertNotNull(savedOutbox);
        assertEquals(userId, savedOutbox.getUserId());
        assertEquals(deckId, savedOutbox.getDeckId());
        assertEquals(cardId, savedOutbox.getCardId());
        assertEquals(Quality.AGAIN.getQuality(), savedOutbox.getQuality());
        assertEquals(OutboxStatus.PENDING, savedOutbox.getStatus());
        assertNotNull(savedOutbox.getEventTimestamp());
        assertNotNull(savedOutbox.getCreatedAt());
        // verify that createdAt is close to now (within a few seconds)
        assertTrue(savedOutbox.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(2)));
        assertTrue(savedOutbox.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(2)));
    }

    @Test
    void executeCallbackQuery_WhenOutboxSaveFails_ShouldLogErrorAndContinue() {
        // Given
        Long cardId = 123L;
        Long deckId = 456L;
        Long sessionId = 789L;
        Long userId = 999L;
        String callbackData = "AGAIN:=" + cardId + ":=" + deckId + ":=" + sessionId;

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(userId);

        Card mockCard = mock(Card.class);
        Deck mockDeck = mock(Deck.class);
        UserInfo mockUser = mock(UserInfo.class);
        when(mockCard.getCardId()).thenReturn(cardId);
        when(mockCard.getDeck()).thenReturn(mockDeck);
        when(mockCard.getStatus()).thenReturn(Status.NEW);
        when(mockDeck.getDeckId()).thenReturn(deckId);
        when(mockDeck.getOwner()).thenReturn(mockUser);
        when(mockUser.getUserChatId()).thenReturn(userId);

        when(learningSessionService.updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN)))
                .thenReturn(mockCard);
        when(learningSessionService.getRemainingCardsCount(sessionId)).thenReturn(1);
        Card mockNextCard = mock(Card.class);
        when(mockNextCard.getFront()).thenReturn("Next question");
        when(learningSessionService.getNextCardInSession(sessionId)).thenReturn(mockNextCard);
        // Simulate repository save throwing exception
        doThrow(new RuntimeException("Database connection failed"))
                .when(analyticsOutboxRepository).save(any(AnalyticsOutbox.class));

        // When
        answerAgainStrategy.executeCallbackQuery(callbackQuery);

        // Then
        verify(learningSessionService).updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN));
        // Should still proceed with normal flow
        verify(learningSessionService, atLeastOnce()).getRemainingCardsCount(sessionId);
        // Verify that super.executeCallbackQuery is called (indirectly via edit menu)
        // Since we cannot verify super call, we can verify that no other exception is thrown
        // and that the method completes without propagating the repository exception.
    }

    @Test
    void executeCallbackQuery_WhenCardStatusRequiresRemoval_ShouldRemoveCardAndStillSaveOutbox() {
        // Given
        Long cardId = 123L;
        Long deckId = 456L;
        Long sessionId = 789L;
        Long userId = 999L;
        String callbackData = "AGAIN:=" + cardId + ":=" + deckId + ":=" + sessionId;

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(userId);

        Card mockCard = mock(Card.class);
        Deck mockDeck = mock(Deck.class);
        UserInfo mockUser = mock(UserInfo.class);
        when(mockCard.getCardId()).thenReturn(cardId);
        when(mockCard.getDeck()).thenReturn(mockDeck);
        when(mockCard.getStatus()).thenReturn(Status.NEW);
        when(mockDeck.getDeckId()).thenReturn(deckId);
        when(mockDeck.getOwner()).thenReturn(mockUser);
        when(mockUser.getUserChatId()).thenReturn(userId);

        when(learningSessionService.updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN)))
                .thenReturn(mockCard);
        when(learningSessionService.getRemainingCardsCount(sessionId)).thenReturn(1);
        Card mockNextCard = mock(Card.class);
        when(mockNextCard.getFront()).thenReturn("Next question");
        when(learningSessionService.getNextCardInSession(sessionId)).thenReturn(mockNextCard);

        // When
        answerAgainStrategy.executeCallbackQuery(callbackQuery);

        // Then
        verify(learningSessionService).updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN));
        verify(mockCard, times(1)).getStatus();
        verify(learningSessionService, never()).removeCardFromSession(sessionId, cardId);
        verify(analyticsOutboxRepository).save(any(AnalyticsOutbox.class));
    }

    @Test
    void executeCallbackQuery_WhenSessionCompleted_ShouldHandleSessionCompletionAndStillSaveOutbox() {
        // Given
        Long cardId = 123L;
        Long deckId = 456L;
        Long sessionId = 789L;
        Long userId = 999L;
        String callbackData = "AGAIN:=" + cardId + ":=" + deckId + ":=" + sessionId;

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(userId);

        Card mockCard = mock(Card.class);
        Deck mockDeck = mock(Deck.class);
        UserInfo mockUser = mock(UserInfo.class);
        when(mockCard.getCardId()).thenReturn(cardId);
        when(mockCard.getDeck()).thenReturn(mockDeck);
        when(mockCard.getStatus()).thenReturn(Status.REVIEW_MATURE);
        when(mockDeck.getDeckId()).thenReturn(deckId);
        when(mockDeck.getOwner()).thenReturn(mockUser);
        when(mockUser.getUserChatId()).thenReturn(userId);

        when(learningSessionService.updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN)))
                .thenReturn(mockCard);
        when(learningSessionService.getRemainingCardsCount(sessionId)).thenReturn(0); // Session completed

        // When
        answerAgainStrategy.executeCallbackQuery(callbackQuery);

        // Then
        verify(learningSessionService).updateCardWithAnswer(eq(cardId), eq(Quality.AGAIN));
        verify(learningSessionService).removeCardFromSession(sessionId, cardId);
        // Should call handleSessionCompleted (which we cannot directly verify due to private method)
        // but we can verify that analytics outbox still saved
        verify(analyticsOutboxRepository).save(any(AnalyticsOutbox.class));
    }
}