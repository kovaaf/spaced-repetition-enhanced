package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.model.MenuMessageState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuMessageStateRepository extends JpaRepository<MenuMessageState, Long> {
    Optional<MenuMessageState> findByChatId(Long chatId);

    void deleteByChatId(Long chatId);
}
