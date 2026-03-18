package org.company.data.service.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.data.model.AnswerEvent;
import org.company.data.repository.AnswerEventRepository;

/**
 * Use case for recording a new answer event.
 */
@Slf4j
@RequiredArgsConstructor
public class RecordAnswerUseCase {
    private final AnswerEventRepository answerEventRepository;

    /**
     * Executes the use case.
     *
     * @param event the answer event to record
     */
    public void execute(AnswerEvent event) {
        log.info("Recording answer event: userId={}, deckId={}, cardId={}, quality={}, timestamp={}",
                event.userId(), event.deckId(), event.cardId(), event.quality(), event.timestamp());
        answerEventRepository.save(event);
    }
}