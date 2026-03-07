package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {
    Optional<Deck> findByNameIgnoreCaseAndOwner(String name, UserInfo owner);

    @EntityGraph(attributePaths = {"cards"})
    Optional<Deck> findWithCardsByNameIgnoreCaseAndOwner(String name, UserInfo owner);

    @EntityGraph(attributePaths = {"cards"})
    Optional<Deck> findWithCardsByDeckId(Long deckId);

    List<Deck> findAllDecksByOwnerUserChatId(Long chatId);

    boolean existsByNameIgnoreCaseAndOwner(String deckName, UserInfo owner);

    Optional<Deck> findByName(String name);

    List<Deck> findByNameEndingWith(String defaultDeckSuffix);
}
