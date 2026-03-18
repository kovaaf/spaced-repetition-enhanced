package org.company.spacedrepetitiondata.service.usecase;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.model.AnswerEvent;
import org.company.spacedrepetitiondata.repository.AnswerEventRepository;

import java.time.Instant;
import java.util.List;

/**
 * Use case for retrieving analytics data.
 */
@Slf4j
@RequiredArgsConstructor
public class GetAnalyticsUseCase {
    private final AnswerEventRepository answerEventRepository;

    @Value
    @Builder
    public static class Request {
        Long userId;
        Instant startTime;
        Instant endTime;
    }

    @Value
    @Builder
    public static class Response {
        List<AnswerEvent> events;
        long totalCount;
    }

    /**
     * Executes the use case.
     *
     * @param request the request parameters
     * @return response containing events and total count
     */
    public Response execute(Request request) {
        log.info("Retrieving analytics for userId={}, startTime={}, endTime={}",
                request.userId, request.startTime, request.endTime);

        List<AnswerEvent> events = answerEventRepository.findByUserAndTimeRange(
                request.userId, request.startTime, request.endTime);
        long totalCount = answerEventRepository.countByUserAndTimeRange(
                request.userId, request.startTime, request.endTime);

        return Response.builder()
                .events(events)
                .totalCount(totalCount)
                .build();
    }
}