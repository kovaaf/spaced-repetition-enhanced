package org.company.spacedrepetitionbot.service;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.model.ExecutedCommand;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.company.spacedrepetitionbot.repository.CommandHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CommandHistoryService {
    private final CommandHistoryRepository commandHistoryRepository;

    public void saveCommand(UserInfo user, String commandId, String[] arguments) {
        ExecutedCommand history = new ExecutedCommand();
        history.setUserInfo(user);
        history.setCommandIdentifier(commandId);
        history.setArguments(arguments);
        history.setExecutedAt(LocalDateTime.now());
        commandHistoryRepository.save(history);
    }

    public Optional<ExecutedCommand> getLastCommand(Long userChatId) {
        return commandHistoryRepository.findTopByUserInfoUserChatIdOrderByExecutedAtDesc(userChatId);
    }

    public Optional<ExecutedCommand> getLastCommandByIdentifier(Long userChatId, String commandIdentifier) {
        return commandHistoryRepository.findTopByUserInfoUserChatIdAndCommandIdentifierOrderByExecutedAtDesc(
                userChatId,
                commandIdentifier);
    }

    public void deleteCommand(Long id) {
        commandHistoryRepository.deleteById(id);
    }
}
