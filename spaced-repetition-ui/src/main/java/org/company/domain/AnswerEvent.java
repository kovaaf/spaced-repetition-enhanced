package org.company.domain;

import java.time.Instant;

/**
 * Represents an answer event from a spaced repetition system.
 * Contains information about the user, deck, card, answer quality and timestamp.
 *
 * @param userId    unique identifier of the user
 * @param userName  display name of the user (may be null)
 * @param deckId    unique identifier of the deck
 * @param deckName  display name of the deck (may be null)
 * @param cardId    unique identifier of the card
 * @param cardTitle title of the card (may be null)
 * @param quality   answer quality (0=Again, 3=Hard, 4=Good, 5=Easy)
 * @param timestamp moment when the answer occurred
 */
public record AnswerEvent(
        String userId,
        String userName,
        String deckId,
        String deckName,
        String cardId,
        String cardTitle,
        int quality,
        Instant timestamp
) { }