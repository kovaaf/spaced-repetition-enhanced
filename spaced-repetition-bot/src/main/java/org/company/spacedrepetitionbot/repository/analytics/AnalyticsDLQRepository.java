package org.company.spacedrepetitionbot.repository.analytics;

import org.company.spacedrepetitionbot.model.analytics.AnalyticsDLQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsDLQRepository extends JpaRepository<AnalyticsDLQ, Long> {
}