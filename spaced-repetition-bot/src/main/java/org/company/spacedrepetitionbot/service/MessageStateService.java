package org.company.spacedrepetitionbot.service;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.model.MenuMessageState;
import org.company.spacedrepetitionbot.repository.MenuMessageStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class MessageStateService {
    private final MenuMessageStateRepository stateRepository;

    public void saveMenuMessageId(Long chatId, Integer messageId) {
        stateRepository.findByChatId(chatId).ifPresentOrElse(
                state -> {
                    state.setMessageId(messageId);
                    stateRepository.save(state);
                },
                () -> stateRepository.save(new MenuMessageState(
                        null,
                        chatId,
                        messageId,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        null)));
    }

    public Integer getMenuMessageId(Long chatId) {
        return stateRepository.findByChatId(chatId).map(MenuMessageState::getMessageId).orElse(null);
    }

    public void clearMenuMessageId(Long chatId) {
        stateRepository.deleteByChatId(chatId);
    }

    public void setUserState(Long chatId, String state) {
        stateRepository.findByChatId(chatId).ifPresentOrElse(
                entity -> {
                    entity.setUserState(state);
                    stateRepository.save(entity);
                }, () -> stateRepository.save(MenuMessageState.builder().chatId(chatId).userState(state).build()));
    }

    public String getUserState(Long chatId) {
        return stateRepository.findByChatId(chatId).map(MenuMessageState::getUserState).orElse(null);
    }

    public void clearUserState(Long chatId) {
        stateRepository.findByChatId(chatId).ifPresent(entity -> {
            entity.setUserState(null);
            stateRepository.save(entity);
        });
    }
}
