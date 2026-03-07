package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.learn;

import jakarta.persistence.EntityNotFoundException;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class ShowAnswerStrategy extends BaseEditCallbackStrategy {
    private final LearningSessionService learningSessionService;
    private final KeyboardManager keyboardManager;
    private final CardService cardService;

    public ShowAnswerStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            LearningSessionService learningSessionService,
            KeyboardManager keyboardManager,
            CardService cardService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.learningSessionService = learningSessionService;
        this.keyboardManager = keyboardManager;
        this.cardService = cardService;
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        messageStateService.clearUserState(callbackQuery.getMessage().getChatId());
        super.executeCallbackQuery(callbackQuery);
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        Long cardId = Long.valueOf(getCallbackDataByIndex(callbackQuery.getData(), 1));

        try {
            String answer = learningSessionService.getCardAnswerById(cardId);

            if (answer.length() > 4096) {
                String errorMessage = "Сообщение слишком длинное для отображения в Telegram\n\n";
                int maxLength = 4096 - errorMessage.length();

                // Проверяем, не обрезаем ли мы середину блока кода
                String truncated = answer.substring(0, maxLength);

                // Ищем последний полный блок кода
                int lastCompleteCodeBlock = findLastCompleteCodeBlock(truncated);

                if (lastCompleteCodeBlock != -1) {
                    // Обрезаем до конца последнего полного блока кода
                    truncated = truncated.substring(0, lastCompleteCodeBlock);
                } else {
                    // Если нет полных блоков кода, ищем начало незавершенного блока
                    int lastCodeBlockStart = truncated.lastIndexOf("```");
                    if (lastCodeBlockStart != -1) {
                        // Удаляем незавершенный блок кода
                        truncated = truncated.substring(0, lastCodeBlockStart);
                    }
                }

                return errorMessage + truncated + "\n\n⚠️ *Текст обрезан, так как не помещается в сообщение Telegram*";
            }

            return answer;
        } catch (EntityNotFoundException e) {
            return "Карточка не найдена";
        }
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long cardId = Long.valueOf(getCallbackDataByIndex(callbackQuery.getData(), 1));
        Long deckId = Long.valueOf(getCallbackDataByIndex(callbackQuery.getData(), 2));
        Long sessionId = Long.valueOf(getCallbackDataByIndex(callbackQuery.getData(), 3));
        Card card = cardService.getCardById(cardId);
        return keyboardManager.getShowAnswerKeyboard(cardId, deckId, sessionId, card.getStatus());
    }

    @Override
    public Callback getPrefix() {
        return Callback.SHOW_ANSWER;
    }

    private int findLastCompleteCodeBlock(String text) {
        int lastCompleteEnd = -1;
        int currentPos = 0;

        while (currentPos < text.length()) {
            int codeBlockStart = text.indexOf("```", currentPos);
            if (codeBlockStart == -1) break;

            int codeBlockEnd = text.indexOf("```", codeBlockStart + 3);
            if (codeBlockEnd == -1) break;

            lastCompleteEnd = codeBlockEnd + 3; // включая закрывающие ```
            currentPos = lastCompleteEnd;
        }

        return lastCompleteEnd;
    }
}
