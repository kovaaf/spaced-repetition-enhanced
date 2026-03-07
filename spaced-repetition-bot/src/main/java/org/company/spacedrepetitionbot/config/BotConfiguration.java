package org.company.spacedrepetitionbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class BotConfiguration {

    @Bean
    public TelegramClient telegramClient(BotConfigurationProperties botProperties) {
        return new OkHttpTelegramClient(botProperties.getBotToken());
    }
}
