package org.company.data.repository;



import org.company.data.model.AnswerEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for answer events.
 */
public interface AnswerEventRepository {
    /**
     * Saves a new answer event.
     *
     * @param event the event to save
     * @return the generated ID, if any
     */
    Optional<Long> save(AnswerEvent event);

    /**
     * Finds answer events by user ID and time range (inclusive).
     *
     * @param userId    the user ID (optional, null means all users)
     * @param startTime start of range (inclusive, optional)
     * @param endTime   end of range (inclusive, optional)
     * @return list of matching events, ordered by timestamp ascending
     */
    List<AnswerEvent> findByUserAndTimeRange(Long userId, Instant startTime, Instant endTime);

    /**
     * Counts answer events by user ID and time range (inclusive).
     *
     * @param userId    the user ID (optional, null means all users)
     * @param startTime start of range (inclusive, optional)
     * @param endTime   end of range (inclusive, optional)
     * @return total count
     */
    long countByUserAndTimeRange(Long userId, Instant startTime, Instant endTime);
}