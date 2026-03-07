package org.company.domain;

import java.time.Instant;

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