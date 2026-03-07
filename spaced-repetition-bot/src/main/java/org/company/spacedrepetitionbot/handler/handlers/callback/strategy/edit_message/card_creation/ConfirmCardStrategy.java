package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.card_creation;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.model.CardDraft;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.company.spacedrepetitionbot.service.CardDraftService;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class ConfirmCardStrategy extends BaseEditCallbackStrategy {
    private final CardDraftService cardDraftService;
    private final CardService cardService;
    private final KeyboardManager keyboardManager;
    private final LearningSessionService learningSessionService;

    public ConfirmCardStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            CardDraftService cardDraftService,
            CardService cardService,
            KeyboardManager keyboardManager,
            LearningSessionService learningSessionService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.cardDraftService = cardDraftService;
        this.cardService = cardService;
        this.keyboardManager = keyboardManager;
        this.learningSessionService = learningSessionService;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        return "✅ Карточка успешно создана!";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        LearningSession session = learningSessionService.getOrCreateSession(deckId);

        int newCards = learningSessionService.countNewCardsInSession(session.getSessionId());
        int reviewCards = learningSessionService.countReviewCardsInSession(session.getSessionId());

        // Возвращаемся в меню колоды
        return keyboardManager.getDeckMenuKeyboard(deckId, newCards, reviewCards);
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();

        try {
            // TODO может передавать в колбэке айдишник черновика? Так надёжнее должно быть
            // сохраняем черновик как карту
            CardDraft draft = cardDraftService.getDraft(chatId).orElseThrow(() -> {
                sendDraftNotFound(chatId, callbackQuery);
                return new IllegalStateException("Draft not found for chat: " + chatId);
            });
            cardService.addCard(draft.getDeckId(), draft.getFront(), draft.getBack());

            // удаляем черновик и сбрасываем состояние пользователя до null
            cardDraftService.clearDraft(chatId);
            messageStateService.clearUserState(chatId);

            super.executeCallbackQuery(callbackQuery);
        } catch (Exception e) {
            log.error("Ошибка подтверждения карты для чата {}: {}", chatId, e.getMessage());
        }
    }

    private void sendDraftNotFound(Long chatId, CallbackQuery callbackQuery) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("❌ Черновик карты не найден")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение об ошибке в чат {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public Callback getPrefix() {
        return Callback.CONFIRM_CARD_CREATION;
    }
}