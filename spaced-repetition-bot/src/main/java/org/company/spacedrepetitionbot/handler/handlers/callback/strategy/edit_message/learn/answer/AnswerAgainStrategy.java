package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.learn.answer;

import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.company.spacedrepetitionbot.handler.handlers.callback.Callback.AGAIN;

@Component
public class AnswerAgainStrategy extends BaseAnswerStrategy {
    protected AnswerAgainStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            LearningSessionService learningSessionService,
            AnalyticsOutboxRepository analyticsOutboxRepository,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService, markdownEscaper, learningSessionService, analyticsOutboxRepository, keyboardManager);
    }

    @Override
    protected Quality getQuality() {
        return Quality.AGAIN;
    }

    @Override
    public Callback getPrefix() {
        return AGAIN;
    }
}
