package org.company.spacedrepetitionbot.utils.mapper;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Slf4j
@Component
public class UserInfoTelegramUserMapper {

    public UserInfo mapTelegramUserToUserInfo(User user) {
        Long telegramUserId = user.getId();
        String telegramUserName = user.getUserName();
        return UserInfo.builder().userChatId(telegramUserId).userName(telegramUserName).build();
    }
}
