package org.company.spacedrepetitionbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.BotConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Slf4j
@Component
public class SpringBot implements SpringLongPollingBot {
    private final BotConfigurationProperties botProperties;
    private final SpacedRepetitionBot spacedRepetitionBot;

    public SpringBot(BotConfigurationProperties botProperties, SpacedRepetitionBot spacedRepetitionBot) {
        this.botProperties = botProperties;
        this.spacedRepetitionBot = spacedRepetitionBot;
    }

    @Override
    public String getBotToken() {
        return botProperties.getBotToken();
    }

    /**
     * Эта реализация возвращает сам бот в качестве обработчика,
     * позволяя ему напрямую обрабатывать входящие обновления.
     *
     * @return этот экземпляр бота в качестве LongPollingUpdateConsumer
     */
    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return spacedRepetitionBot;
    }
}
