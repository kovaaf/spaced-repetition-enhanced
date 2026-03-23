package org.company.data.service.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.data.model.AnswerEvent;
import org.company.data.model.Quality;
import org.company.data.model.User;
import org.company.data.service.health.MetricsEndpoint;
import org.company.data.service.usecase.GetAnalyticsUseCase;
import org.company.data.service.usecase.GetUsersUseCase;
import org.company.data.service.usecase.RecordAnswerUseCase;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;

import java.time.Instant;
import java.util.List;

/**
 * gRPC service implementation for AnalyticsService.
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyticsGrpcService extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {
    private final RecordAnswerUseCase recordAnswerUseCase;
    private final GetAnalyticsUseCase getAnalyticsUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final MetricsEndpoint metricsEndpoint;
    private final StreamingStrategy streamingStrategy;

    @Override
    public void recordAnswerEvent(AnalyticsProto.AnswerEvent request,
            StreamObserver<Empty> responseObserver) {
        metricsEndpoint.incrementRecordAnswerEventRequests();
        try {
            validateRecordAnswerRequest(request);

            AnswerEvent event = AnswerEvent.builder()
                    .userId(Long.parseLong(request.getUserId()))
                    .deckId(Long.parseLong(request.getDeckId()))
                    .cardId(Long.parseLong(request.getCardId()))
                    .quality(request.getQualityValue())
                    .timestamp(Instant.ofEpochSecond(
                            request.getTimestamp().getSeconds(),
                            request.getTimestamp().getNanos()))
                    .userName(request.hasUserName() ? request.getUserName() : null)
                    .deckName(request.hasDeckName() ? request.getDeckName() : null)
                    .cardTitle(request.hasCardTitle() ? request.getCardTitle() : null)
                    .build();

            recordAnswerUseCase.execute(event);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.info("RecordAnswerEvent processed successfully");
        } catch (NumberFormatException e) {
            metricsEndpoint.incrementRecordAnswerEventErrors();
            responseObserver.onError(new StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("Invalid ID format: must be numeric").withCause(e)));
        } catch (IllegalArgumentException e) {
            metricsEndpoint.incrementRecordAnswerEventErrors();
            responseObserver.onError(new StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e)));
        } catch (Exception e) {
            metricsEndpoint.incrementRecordAnswerEventErrors();
            log.error("Error processing RecordAnswerEvent", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription("Internal server error").withCause(e)));
        }
    }

    @Override
    public void getAnalytics(AnalyticsProto.AnalyticsRequest request,
            StreamObserver<AnalyticsProto.AnalyticsResponse> responseObserver) {
        metricsEndpoint.incrementGetAnalyticsRequests();
        try {
            validateAnalyticsRequest(request);

            Long userId = request.getUserId().isEmpty() ? null : Long.parseLong(request.getUserId());
            Instant startTime = Instant.ofEpochSecond(
                    request.getStartTime().getSeconds(),
                    request.getStartTime().getNanos());
            Instant endTime = Instant.ofEpochSecond(
                    request.getEndTime().getSeconds(),
                    request.getEndTime().getNanos());

            GetAnalyticsUseCase.Request useCaseRequest = GetAnalyticsUseCase.Request.builder()
                    .userId(userId)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
            GetAnalyticsUseCase.Response useCaseResponse = getAnalyticsUseCase.execute(useCaseRequest);

            AnalyticsProto.AnalyticsResponse.Builder responseBuilder = AnalyticsProto.AnalyticsResponse.newBuilder();
            for (AnswerEvent event : useCaseResponse.getEvents()) {
                responseBuilder.addEvents(toProto(event));
            }
            responseBuilder.setTotalCount((int) useCaseResponse.getTotalCount());
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("GetAnalytics processed: returned {} events", useCaseResponse.getEvents().size());
        } catch (NumberFormatException e) {
            metricsEndpoint.incrementGetAnalyticsErrors();
            responseObserver.onError(new StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("Invalid user ID format").withCause(e)));
        } catch (IllegalArgumentException e) {
            metricsEndpoint.incrementGetAnalyticsErrors();
            responseObserver.onError(new StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e)));
        } catch (Exception e) {
            metricsEndpoint.incrementGetAnalyticsErrors();
            log.error("Error processing GetAnalytics", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription("Internal server error").withCause(e)));
        }
    }

    @Override
    public void getUsers(Empty request,
            StreamObserver<AnalyticsProto.UsersResponse> responseObserver) {
        metricsEndpoint.incrementGetAnalyticsRequests(); // reuse counter
        try {
            List<User> users = getUsersUseCase.execute();
            AnalyticsProto.UsersResponse.Builder responseBuilder = AnalyticsProto.UsersResponse.newBuilder();
            for (User user : users) {
                responseBuilder.addUsers(AnalyticsProto.User.newBuilder()
                        .setId(user.id())
                        .setName(user.name() != null ? user.name() : "")
                        .build());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("GetUsers processed: returned {} users", users.size());
        } catch (Exception e) {
            metricsEndpoint.incrementGetAnalyticsErrors();
            log.error("Error processing GetUsers", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription("Internal server error").withCause(e)));
        }
    }

    @Override
    public void streamAnalytics(AnalyticsProto.StreamAnalyticsRequest request,
            StreamObserver<AnalyticsProto.StreamAnalyticsResponse> responseObserver) {
        streamingStrategy.stream(request, responseObserver);
    }

    // ---- Validation methods ----

    private void validateRecordAnswerRequest(AnalyticsProto.AnswerEvent request) {
        if (request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("user_id is required");
        }
        if (request.getDeckId().isEmpty()) {
            throw new IllegalArgumentException("deck_id is required");
        }
        if (request.getCardId().isEmpty()) {
            throw new IllegalArgumentException("card_id is required");
        }
        if (!request.hasTimestamp()) {
            throw new IllegalArgumentException("timestamp is required");
        }
        int quality = request.getQualityValue();
        Quality.fromValue(quality); // throws if invalid
    }

    private void validateAnalyticsRequest(AnalyticsProto.AnalyticsRequest request) {
        if (!request.hasStartTime()) {
            throw new IllegalArgumentException("start_time is required");
        }
        if (!request.hasEndTime()) {
            throw new IllegalArgumentException("end_time is required");
        }
        Instant start = Instant.ofEpochSecond(request.getStartTime().getSeconds(), request.getStartTime().getNanos());
        Instant end = Instant.ofEpochSecond(request.getEndTime().getSeconds(), request.getEndTime().getNanos());
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end_time must not be before start_time");
        }
    }

    // ---- Conversion methods ----

    private AnalyticsProto.AnswerEvent toProto(AnswerEvent event) {
        AnalyticsProto.AnswerEvent.Builder builder = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId(String.valueOf(event.userId()))
                .setDeckId(String.valueOf(event.deckId()))
                .setCardId(String.valueOf(event.cardId()))
                .setUserName(String.valueOf(event.userName()))
                .setDeckName(String.valueOf(event.deckName()))
                .setCardTitle(String.valueOf(event.cardTitle()))
                .setQualityValue(event.quality())
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(event.timestamp().getEpochSecond())
                        .setNanos(event.timestamp().getNano())
                        .build());
        if (event.userName() != null) {
            builder.setUserName(event.userName());
        }
        if (event.deckName() != null) {
            builder.setDeckName(event.deckName());
        }
        if (event.cardTitle() != null) {
            builder.setCardTitle(event.cardTitle());
        }
        return builder.build();
    }
}