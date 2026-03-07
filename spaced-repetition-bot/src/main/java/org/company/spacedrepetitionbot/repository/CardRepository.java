package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByDeckAndFrontIgnoreCase(Deck deck, String front);

    List<Card> findByDeck(Deck deck);

    boolean existsByDeckAndFrontIgnoreCase(Deck targetDeck, String cardFront);

    Optional<Card> findBySourceFilePathAndSourceHeading(String sourceFilePath, String sourceHeading);

    int countByDeck(Deck deck);

    @Modifying
    @Query("DELETE FROM Card c WHERE c.deck = :deck AND c.sourceFilePath NOT IN :processedFilePaths")
    int deleteByDeckAndSourceFilePathNotIn(Deck deck, Set<String> processedFilePaths);

    void deleteByDeckAndSourceFilePathAndFrontNotIn(Deck deck, String relativePath, List<String> validFronts);

    void deleteByDeckAndSourceFilePath(Deck deck, String relativePath);

    int countByDeckAndSourceFilePath(Deck deck, String filePath);

    @Query("SELECT c FROM Card c " +
            "WHERE c.deck = :deck " +
            "AND c.status IN :statuses " +
            "ORDER BY CASE " +
            "   WHEN c.status IN (org.company.spacedrepetitionbot.constants.Status.LEARNING, " +
            "                    org.company.spacedrepetitionbot.constants.Status.RELEARNING) THEN 1 " +
            "   WHEN c.status = org.company.spacedrepetitionbot.constants.Status.NEW THEN 2 " +
            "   ELSE 3 END, c.nextReviewTime ASC")
    List<Card> findCardsForSession(Deck deck, List<Status> statuses, Pageable pageable);

    @Query("SELECT c FROM Card c " +
            "WHERE c.deck = :deck " +
            "AND c.status IN ('REVIEW_YOUNG', 'REVIEW_MATURE') " +
            "AND c.nextReviewTime <= :now " +
            "ORDER BY c.nextReviewTime ASC")
    List<Card> findOverdueReviewCards(Deck deck, LocalDateTime now, Pageable pageable);
}
