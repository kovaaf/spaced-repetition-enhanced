package org.company.spacedrepetitionbot.command.general;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.service.UserInfoService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.company.spacedrepetitionbot.command.constant.Command.START;
import static org.company.spacedrepetitionbot.constants.MessageConstants.USER_INITIALIZATION_ERROR;

@Slf4j
@Component
public class StartCommand extends SpacedRepetitionCommand {
    private final UserInfoService userInfoService;

    public StartCommand(UserInfoService userInfoService) {
        super(START.getAlias(), START.getDescription(), START.getExtendedDescription(), START.getValidArgumentCounts());
        this.userInfoService = userInfoService;
    }

    @Override
    protected void performAction(TelegramClient telegramClient, String[] arguments) {
        try {
            User currentUser = getCurrentUser();
            String result = userInfoService.initializeAndGreetUser(currentUser);
            sendMessage(telegramClient, result);
        } catch (Exception e) {
            log.error("Ошибка инициализации пользователя: {}", e.getMessage());
            String result = String.format(USER_INITIALIZATION_ERROR.getMessage(), e.getMessage());
            sendMessage(telegramClient, result);
        }
    }
}