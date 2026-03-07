package org.company.spacedrepetitionbot.command.general;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.UserInfoService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.company.spacedrepetitionbot.command.constant.Command.MENU;
import static org.company.spacedrepetitionbot.constants.MessageConstants.USER_INITIALIZATION_ERROR;

/**
 * Команда для вызова кнопочного меню
 */
@Slf4j
@Component
public class MenuCommand extends SpacedRepetitionCommand {
    private final MessageStateService messageStateService;
    private final KeyboardManager keyboardManager;
    private final UserInfoService userInfoService;

    public MenuCommand(
            MessageStateService messageStateService,
            KeyboardManager keyboardManager,
            UserInfoService userInfoService) {
        super(MENU.getAlias(), MENU.getDescription(), MENU.getExtendedDescription(), MENU.getValidArgumentCounts());
        this.messageStateService = messageStateService;
        this.keyboardManager = keyboardManager;
        this.userInfoService = userInfoService;
    }

    /**
     * Возвращает кнопочное меню
     *
     * @param arguments массив аргументов команды, где:
     *                  arguments[0] - название колоды
     */
    @Override
    protected void performAction(TelegramClient telegramClient, String[] arguments) {
        // TODO должен отображаться список кнопок: "Список колод", "Завершить общение"
        //  "Список колод" отображает список всех колод пользователя в виде кнопок с названиями колод, в начале
        //  списка кнопки "Назад в главное меню", "Завершить общение"
        //  В колодах кнопки: "Учить", "Добавить карту", "Список всех карт", "Назад к списку колод", "Завершить общение"
        //  "Список всех карт" отображает список всех карт в колоде пользователя в виде кнопок с front из колод, в
        //  начале списка кнопки "Назад в колоду", "Завершить общение"
        //  В "учить" возвращает следующую карточку для изучения из указанной колоды с кнопками: "показать ответ" -
        //  первой строкой, второй - "skip", "previous", "edit", "exclude"(он же suspended статус), "Назад к списку
        //  колод", "Завершить общение"
        //  В "показать ответ" кнопки: "again", "hard", "good", "easy" - первой строкой, второй - "skip", "previous",
        //  "edit", "exclude"(он же suspended статус), "Назад к списку колод", "Завершить общение"

        try {
            User currentUser = getCurrentUser();
            userInfoService.initializeAndGreetUser(currentUser);
        } catch (Exception e) {
            log.error("Ошибка инициализации пользователя: {}", e.getMessage());
            String result = String.format(USER_INITIALIZATION_ERROR.getMessage(), e.getMessage());
            sendMessage(telegramClient, result);
        }

        Message message = sendMessage(telegramClient, "Выбери действие:", keyboardManager.getMainMenuKeyboard());
        messageStateService.saveMenuMessageId(chatId, message.getMessageId());
    }
}
