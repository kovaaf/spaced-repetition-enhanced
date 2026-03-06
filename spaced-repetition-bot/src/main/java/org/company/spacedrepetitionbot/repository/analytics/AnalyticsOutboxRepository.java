package org.company.spacedrepetitionbot.repository.analytics;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnalyticsOutboxRepository extends JpaRepository<AnalyticsOutbox, Long> {

    List<AnalyticsOutbox> findByStatus(OutboxStatus status);

    @Query("SELECT ao FROM AnalyticsOutbox ao WHERE ao.status = :status ORDER BY ao.eventId ASC")
    List<AnalyticsOutbox> findByStatusOrderByEventIdAsc(@Param("status") OutboxStatus status, Pageable pageable);

    Optional<AnalyticsOutbox> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(value = {@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT ao FROM AnalyticsOutbox ao WHERE ao.status IN :statuses AND (ao.nextRetryAt IS NULL OR ao.nextRetryAt <= CURRENT_TIMESTAMP) ORDER BY ao.eventId ASC")
    List<AnalyticsOutbox> findPendingForProcessing(@Param("statuses") List<OutboxStatus> statuses, Pageable pageable);
}