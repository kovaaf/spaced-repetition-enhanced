package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.model.CardDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardDraftRepository extends JpaRepository<CardDraft, Long> {
    Optional<CardDraft> findByChatId(Long chatId);

    void deleteByChatId(Long chatId);
}
