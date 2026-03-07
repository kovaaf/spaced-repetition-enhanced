package org.company.spacedrepetitionbot.service;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.company.spacedrepetitionbot.repository.UserInfoRepository;
import org.company.spacedrepetitionbot.utils.mapper.UserInfoTelegramUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.company.spacedrepetitionbot.constants.MessageConstants.WELCOME_MESSAGE;

@Slf4j
@Service
public class UserInfoService {
    private final UserInfoRepository userInfoRepository;
    private final UserInfoTelegramUserMapper userInfoTelegramUserMapper;

    public UserInfoService(
            UserInfoRepository userInfoRepository,
            UserInfoTelegramUserMapper userInfoTelegramUserMapper) {
        this.userInfoRepository = userInfoRepository;
        this.userInfoTelegramUserMapper = userInfoTelegramUserMapper;
    }

    /**
     * Инициализация пользователя и получение приветственного сообщения
     *
     * @param telegramUser пользователь Telegram
     * @return приветственное сообщение с именем пользователя
     */
    @Transactional
    public String initializeAndGreetUser(User telegramUser) {
        Long userChatId = telegramUser.getId();
        userInfoRepository.findById(userChatId)
                .ifPresentOrElse(
                        existingUser -> updateUsernameIfChanged(existingUser, telegramUser),
                        () -> getOrCreate(telegramUser));
        return formatWelcomeMessage(telegramUser);
    }

    private void updateUsernameIfChanged(UserInfo userInfo, User telegramUser) {
        if (!userInfo.getUserName().equals(telegramUser.getUserName())) {
            userInfo.setUserName(telegramUser.getUserName());
            userInfoRepository.save(userInfo);
            log.debug("Обновлено имя пользователя для: {}", telegramUser.getId());
        }
    }

    public UserInfo getOrCreate(User telegramUser) {
        UserInfo newUser = userInfoTelegramUserMapper.mapTelegramUserToUserInfo(telegramUser);
        UserInfo userInfo = userInfoRepository.save(newUser);
        log.debug("Создан новый пользователь: {}", telegramUser.getId());
        return userInfo;
    }

    private String formatWelcomeMessage(User user) {
        String userName = determineUserName(user);
        return String.format(WELCOME_MESSAGE.getMessage(), userName);
    }

    private String determineUserName(User user) {
        return (user.getFirstName().length() > 3) ? user.getFirstName() : user.getUserName();
    }

    @Transactional
    public UserInfo getSystemUser() {
        return userInfoRepository.findByUserName("system")
                .orElseGet(() -> userInfoRepository.save(UserInfo.builder().userName("system").userChatId(0L) // 0 - ID
                        // системного пользователя
                        .build()));
    }

    @Transactional
    public boolean hasUserCopiedDefaultDeck(Long chatId) {
        return userInfoRepository.findById(chatId).map(UserInfo::isHasCopiedDefaultDeck).orElse(false);
    }

    @Transactional
    public void markUserCopiedDefaultDeck(Long chatId) {
        userInfoRepository.findById(chatId).ifPresent(user -> {
            user.setHasCopiedDefaultDeck(true);
            userInfoRepository.save(user);
            log.debug("Пользователь {} помечен как скопировавший дефолтную колоду", chatId);
        });
    }
}
