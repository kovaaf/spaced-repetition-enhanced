package org.company.spacedrepetitionbot.command.general;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.DefaultBotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.helpCommand.IManCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Set;
import java.util.stream.Collectors;

import static org.company.spacedrepetitionbot.constants.MessageConstants.*;

@Slf4j
public abstract class SpacedRepetitionCommand extends DefaultBotCommand implements IManCommand {
    private static final String LINE_SEPARATOR = "\n-----------------\n";
    private final String extendedDescription;
    private final Set<Integer> validArgumentCounts;
    protected Long chatId;
    private User currentUser;

    public SpacedRepetitionCommand(
            String commandIdentifier,
            String description,
            String extendedDescription,
            Set<Integer> validArgumentCounts) {
        super(commandIdentifier, description);
        this.extendedDescription = extendedDescription;
        this.validArgumentCounts = validArgumentCounts;
    }

    protected abstract void performAction(TelegramClient telegramClient, String[] arguments);

    @Override
    public void execute(TelegramClient telegramClient, User user, Chat chat, Integer messageId, String[] arguments) {
        this.chatId = chat.getId();
        this.currentUser = user;
        log.debug(DEBUG_EXECUTION_TEMPLATE_MESSAGE.getMessage(), getCommandIdentifier(), user, chat, arguments);

        // 1. Проверка количества аргументов
        if (!validateArgumentCount(arguments)) {
            sendMessage(telegramClient, generateArgumentCountError(arguments));
            return;
        }

        // 2. Проверка всех аргументов на пустоту
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] == null || arguments[i].trim().isEmpty()) {
                sendMessage(
                        telegramClient,
                        String.format(EMPTY_ARGUMENT_TEMPLATE_MESSAGE.getMessage(), ERROR_MESSAGE.getMessage(), i + 1));
                return;
            }
        }

        performAction(telegramClient, arguments);
    }

    protected User getCurrentUser() {
        return currentUser;
    }

    protected Message sendMessage(TelegramClient telegramClient, String text, InlineKeyboardMarkup keyboard) {
        var message = SendMessage.builder().chatId(chatId) // should not be null
                .text(text) // should not be null
                .parseMode("HTML").replyMarkup(keyboard).build();

        try {
            return telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
        return null;
    }

    protected Message sendMessage(TelegramClient telegramClient, String text) {
        return sendMessage(telegramClient, text, null);
    }

    @Override
    public String getExtendedDescription() {
        return extendedDescription;
    }

    @Override
    public String toMan() {
        return String.join(LINE_SEPARATOR, toString(), extendedDescription != null ? extendedDescription : "");
    }

    private String generateArgumentCountError(String[] arguments) {
        if (validArgumentCounts.size() == 1) {
            int validCounts = validArgumentCounts.iterator().next();
            return String.format(
                    WRONG_ARGUMENTS_TEMPLATE_MESSAGE.getMessage(),
                    arguments.length,
                    validCounts,
                    getExtendedDescription());
        } else {
            String validCounts = validArgumentCounts.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" или "));

            return String.format(
                    WRONG_ARGUMENTS_COUNT_MULTIPLE.getMessage(),
                    arguments.length,
                    validCounts,
                    getExtendedDescription());
        }
    }

    private boolean validateArgumentCount(String[] arguments) {
        return validArgumentCounts.contains(arguments.length);
    }
}
