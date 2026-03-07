package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.model.ExecutedCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommandHistoryRepository extends JpaRepository<ExecutedCommand, Long> {
    Optional<ExecutedCommand> findTopByUserInfoUserChatIdOrderByExecutedAtDesc(Long userChatId);

    Optional<ExecutedCommand> findTopByUserInfoUserChatIdAndCommandIdentifierOrderByExecutedAtDesc(
            Long userChatId,
            String commandIdentifier);
}
