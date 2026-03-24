package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CardRepository extends JpaRepository<Card, Long> {

    // Поиск карточки по колоде и фронту (только активные)
    @Query("SELECT c FROM Card c WHERE c.deck = :deck AND LOWER(c.front) = LOWER(:front) AND c.deletedAt IS NULL")
    Optional<Card> findByDeckAndFrontIgnoreCase(@Param("deck") Deck deck, @Param("front") String front);

    // Все активные карточки колоды
    @Query("SELECT c FROM Card c WHERE c.deck = :deck AND c.deletedAt IS NULL")
    List<Card> findByDeck(@Param("deck") Deck deck);

    // Проверка существования активной карточки
    @Query("SELECT COUNT(c) > 0 FROM Card c WHERE c.deck = :deck AND LOWER(c.front) = LOWER(:front) AND c.deletedAt IS NULL")
    boolean existsByDeckAndFrontIgnoreCase(@Param("deck") Deck deck, @Param("front") String front);

    // Поиск по sourceFilePath и sourceHeading (активные)
    @Query("SELECT c FROM Card c WHERE c.sourceFilePath = :filePath AND c.sourceHeading = :heading AND c.deletedAt IS NULL")
    Optional<Card> findBySourceFilePathAndSourceHeading(@Param("filePath") String filePath, @Param("heading") String heading);

    // Количество активных карточек в колоде
    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck = :deck AND c.deletedAt IS NULL")
    int countByDeck(@Param("deck") Deck deck);

    // Количество активных карточек в колоде для конкретного файла
    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck = :deck AND c.sourceFilePath = :filePath AND c.deletedAt IS NULL")
    int countByDeckAndSourceFilePath(@Param("deck") Deck deck, @Param("filePath") String filePath);

    // Мягкое удаление карточек, не входящих в список обработанных путей
    @Modifying
    @Query("UPDATE Card c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.deck = :deck AND c.sourceFilePath NOT IN :processedFilePaths AND c.deletedAt IS NULL")
    int softDeleteByDeckAndSourceFilePathNotIn(@Param("deck") Deck deck, @Param("processedFilePaths") Set<String> processedFilePaths);

    // Мягкое удаление всех карточек конкретного файла
    @Modifying
    @Query("UPDATE Card c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.deck = :deck AND c.sourceFilePath = :filePath AND c.deletedAt IS NULL")
    int softDeleteByDeckAndSourceFilePath(@Param("deck") Deck deck, @Param("filePath") String filePath);

    // Мягкое удаление карточек из файла, чьи фронты отсутствуют в списке
    @Modifying
    @Query("UPDATE Card c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.deck = :deck AND c.sourceFilePath = :filePath AND c.front NOT IN :validFronts AND c.deletedAt IS NULL")
    int softDeleteByDeckAndSourceFilePathAndFrontNotIn(@Param("deck") Deck deck, @Param("filePath") String filePath, @Param("validFronts") List<String> validFronts);

    // Восстановление карточки (снятие пометки удаления)
    @Modifying
    @Query("UPDATE Card c SET c.deletedAt = NULL WHERE c.deck = :deck AND c.sourceFilePath = :filePath AND c.sourceHeading = :heading")
    int restoreBySource(@Param("deck") Deck deck, @Param("filePath") String filePath, @Param("heading") String heading);

    // Поиск карточек для сессии (только активные)
    @Query("SELECT c FROM Card c " +
            "WHERE c.deck = :deck " +
            "AND c.deletedAt IS NULL " +
            "AND c.status IN :statuses " +
            "ORDER BY CASE " +
            "   WHEN c.status IN (org.company.spacedrepetitionbot.constants.Status.LEARNING, " +
            "                    org.company.spacedrepetitionbot.constants.Status.RELEARNING) THEN 1 " +
            "   WHEN c.status = org.company.spacedrepetitionbot.constants.Status.NEW THEN 2 " +
            "   ELSE 3 END, c.nextReviewTime ASC")
    List<Card> findCardsForSession(@Param("deck") Deck deck, @Param("statuses") List<Status> statuses, Pageable pageable);

    // Поиск просроченных карточек для ревью (только активные)
    @Query("SELECT c FROM Card c " +
            "WHERE c.deck = :deck " +
            "AND c.deletedAt IS NULL " +
            "AND c.status IN ('REVIEW_YOUNG', 'REVIEW_MATURE') " +
            "AND c.nextReviewTime <= :now " +
            "ORDER BY c.nextReviewTime ASC")
    List<Card> findOverdueReviewCards(@Param("deck") Deck deck, @Param("now") LocalDateTime now, Pageable pageable);

    // Получение всех карточек колоды (включая удалённые) – для административных целей
    @Query("SELECT c FROM Card c WHERE c.deck = :deck")
    List<Card> findAllByDeckIncludingDeleted(@Param("deck") Deck deck);

    // Физическое удаление (если потребуется, но лучше не использовать)
    // Оставляем на крайний случай, но в обычной работе не вызываем
    @Modifying
    @Query("DELETE FROM Card c WHERE c.deck = :deck AND c.sourceFilePath NOT IN :processedFilePaths")
    int deleteByDeckAndSourceFilePathNotInPhysical(@Param("deck") Deck deck, @Param("processedFilePaths") Set<String> processedFilePaths);
}