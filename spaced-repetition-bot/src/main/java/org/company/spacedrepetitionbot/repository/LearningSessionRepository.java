package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {
    Optional<LearningSession> findByDeck(Deck deck);
}
