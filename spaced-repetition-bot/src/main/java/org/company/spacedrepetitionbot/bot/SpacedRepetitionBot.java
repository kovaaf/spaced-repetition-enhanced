package org.company.spacedrepetitionbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.command.general.SpacedRepetitionCommand;
import org.company.spacedrepetitionbot.config.BotConfigurationProperties;
import org.company.spacedrepetitionbot.handler.UpdateDispatcher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class SpacedRepetitionBot extends CommandLongPollingTelegramBot {
    private final UpdateDispatcher updateDispatcher;

    public SpacedRepetitionBot(
            TelegramClient telegramClient,
            BotConfigurationProperties botProperties,
            UpdateDispatcher updateDispatcher,
            SpacedRepetitionCommand... commands) {
        super(telegramClient, true, botProperties::getBotName);
        this.registerAll(commands);
        this.updateDispatcher = updateDispatcher;
    }

    /**
     * Обработка всех обновлений, которые не являются командами.
     *
     * @param update обновление
     * @warning Команды, которые имеют корректный синтаксис, но не зарегистрированы на этом боте, не будут
     * форвардиться в этот метод, если есть дефолтное действие.
     */
    @Override
    public void processNonCommandUpdate(Update update) {
        updateDispatcher.handle(update);
    }

    /**
     * Этот метод вызывается, когда пользователь отправляет незарегистрированную команду.
     * По умолчанию он просто вызывает processNonCommandUpdate(),
     * переопределите его в своей реализации, если хотите, чтобы ваш бот выполнял другие действия, например,
     * отправлял сообщение об ошибке.
     *
     * @param update Полученное обновление от Telegram
     */
    @Override
    public void processInvalidCommandUpdate(Update update) {
        super.processInvalidCommandUpdate(update);
    }

    /**
     * Переопределите этот метод в реализации вашего бота, чтобы фильтровать сообщения с командами
     * Например, если вы хотите запретить выполнение команд, приходящих из чата группы:
     * return !message.getChat().isGroupChat();
     *
     * @param message Полученное сообщение
     * @return true, если сообщение должно быть проигнорировано ботом и обработано как не-команда, false в противном
     * случае
     * @note Реализация по умолчанию не фильтрует ничего
     */
    @Override
    public boolean filter(Message message) {
        return super.filter(message);
    }
}
