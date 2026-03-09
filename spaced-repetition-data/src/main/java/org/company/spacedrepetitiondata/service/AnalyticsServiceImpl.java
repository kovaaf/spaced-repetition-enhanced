package org.company.spacedrepetitiondata.service;

import com.google.protobuf.Empty;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;
import org.company.spacedrepetitiondata.health.MetricsEndpoint;
import org.company.spacedrepetitiondata.repository.AnswerEventRecord;
import org.company.spacedrepetitiondata.repository.AnswerEventRepository;
import org.company.spacedrepetitiondata.repository.UserRecord;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of AnalyticsService with metrics tracking.
 */
@Slf4j
public class AnalyticsServiceImpl extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {
    // Quality constants for spaced repetition ratings
    private static final int QUALITY_AGAIN = 0;
    private static final int QUALITY_HARD = 3;
    private static final int QUALITY_GOOD = 4;
    private static final int QUALITY_EASY = 5;
    
    // Streaming constants
    private static final int STREAMING_POLLING_INTERVAL_SECONDS = 5;
    private static final int STREAMING_INITIAL_DELAY_SECONDS = 5;
    
    private final MetricsEndpoint metricsEndpoint;
    private final AnswerEventRepository answerEventRepository;
    
    public AnalyticsServiceImpl(AnswerEventRepository answerEventRepository, MetricsEndpoint metricsEndpoint) {
        this.metricsEndpoint = metricsEndpoint;
        this.answerEventRepository = answerEventRepository != null ? answerEventRepository : new AnswerEventRepository();
    }
    
    @Override
    public void recordAnswerEvent(AnalyticsProto.AnswerEvent request,
                                  StreamObserver<Empty> responseObserver) {
        try {
            // Track metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementRecordAnswerEventRequests();
            }
            
            log.info("RecordAnswerEvent received: user={}, deck={}, card={}, quality={}", 
                        request.getUserId(), request.getDeckId(), request.getCardId(), request.getQuality());
            
            // Validate required fields
            if (request.getUserId().isEmpty()) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("user_id is required"));
            }
            if (request.getDeckId().isEmpty()) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("deck_id is required"));
            }
            if (request.getCardId().isEmpty()) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("card_id is required"));
            }
            // Validate quality value (must be 0, 3, 4, or 5)
            int qualityValue = request.getQualityValue();
            if (qualityValue != QUALITY_AGAIN && qualityValue != QUALITY_HARD && qualityValue != QUALITY_GOOD && qualityValue != QUALITY_EASY) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(
                        "quality must be 0 (AGAIN), 3 (HARD), 4 (GOOD), or 5 (EASY)"));
            }
            // Validate timestamp is present (seconds or nanos may be zero, but that's valid)
            if (!request.hasTimestamp()) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("timestamp is required"));
            }
            
            // Convert protobuf to record and store
            AnswerEventRecord record = AnswerEventRepository.fromProto(request);
            answerEventRepository.insert(record);
            
            // Send empty response
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            
            log.info("RecordAnswerEvent processed successfully");
        } catch (Exception e) {
            // Track error metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementRecordAnswerEventErrors();
            }
            
            log.error("Error processing RecordAnswerEvent", e);
            
            // Map to appropriate gRPC status
            StatusRuntimeException statusException;
            if (e instanceof StatusRuntimeException) {
                statusException = (StatusRuntimeException) e;
            } else if (e instanceof NumberFormatException) {
                statusException = new StatusRuntimeException(Status.INVALID_ARGUMENT
                        .withDescription("Invalid ID format: must be numeric")
                        .withCause(e));
            } else if (e instanceof RuntimeException) {
                statusException = new StatusRuntimeException(Status.INTERNAL
                        .withDescription("Failed to store answer event")
                        .withCause(e));
            } else {
                statusException = new StatusRuntimeException(Status.INTERNAL
                        .withDescription("Unexpected error")
                        .withCause(e));
            }
            responseObserver.onError(statusException);
        }
    }

    @Override
    public void getAnalytics(AnalyticsProto.AnalyticsRequest request,
                             StreamObserver<AnalyticsProto.AnalyticsResponse> responseObserver) {
        try {
            log.info("GetAnalytics method invoked");
            // Track metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementGetAnalyticsRequests();
            }
            
            log.info("GetAnalytics received: user={}, startTime={}, endTime={}", 
                        request.getUserId(), request.getStartTime(), request.getEndTime());
            
            // Validate required fields
            if (!request.hasStartTime()) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("start_time is required"));
            }
            if (!request.hasEndTime()) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("end_time is required"));
            }
            
            // Parse user ID (optional, empty means all users)
            Long userId = null;
            if (!request.getUserId().isEmpty()) {
                try {
                    userId = Long.parseLong(request.getUserId());
                } catch (NumberFormatException e) {
                    throw new StatusRuntimeException(Status.INVALID_ARGUMENT
                            .withDescription("Invalid user_id format: must be numeric")
                            .withCause(e));
                }
            }
            
            // Convert timestamps to Instant
            Instant startInstant = Instant.ofEpochSecond(
                    request.getStartTime().getSeconds(),
                    request.getStartTime().getNanos());
            Instant endInstant = Instant.ofEpochSecond(
                    request.getEndTime().getSeconds(),
                    request.getEndTime().getNanos());
            
            // Validate time range
            if (endInstant.isBefore(startInstant)) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT
                        .withDescription("end_time must not be before start_time"));
            }
            
            // Query repository
            List<AnswerEventRecord> events;
            long totalCount;
            if (userId != null) {
                log.info("Querying answer events for user {} in time range {} - {}", 
                            userId, startInstant, endInstant);
                events = answerEventRepository.findByUserIdAndTimeRange(userId, startInstant, endInstant);
                totalCount = answerEventRepository.countByUserIdAndTimeRange(userId, startInstant, endInstant);
            } else {
                log.info("Querying answer events for all users in time range {} - {}", 
                            startInstant, endInstant);
                events = answerEventRepository.findByTimeRange(startInstant, endInstant);
                totalCount = answerEventRepository.countByTimeRange(startInstant, endInstant);
            }
            
            // Build response
            AnalyticsProto.AnalyticsResponse.Builder responseBuilder = AnalyticsProto.AnalyticsResponse.newBuilder();
            for (AnswerEventRecord record : events) {
                responseBuilder.addEvents(AnswerEventRepository.toProto(record));
            }
            responseBuilder.setTotalCount((int) totalCount); // safe cast because count fits in int32 per schema
            AnalyticsProto.AnalyticsResponse response = responseBuilder.build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.info("GetAnalytics processed successfully: returned {} events, total count {}", 
                       events.size(), totalCount);
        } catch (Exception e) {
            // Track error metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementGetAnalyticsErrors();
            }
            
            log.error("Error processing GetAnalytics", e);
            
            // Map to appropriate gRPC status
            StatusRuntimeException statusException;
            if (e instanceof StatusRuntimeException) {
                statusException = (StatusRuntimeException) e;
            } else if (e instanceof NumberFormatException) {
                statusException = new StatusRuntimeException(Status.INVALID_ARGUMENT
                        .withDescription("Invalid ID format: must be numeric")
                        .withCause(e));
            } else if (e instanceof RuntimeException) {
                statusException = new StatusRuntimeException(Status.INTERNAL
                        .withDescription("Failed to retrieve analytics")
                        .withCause(e));
            } else {
                statusException = new StatusRuntimeException(Status.INTERNAL
                        .withDescription("Unexpected error")
                        .withCause(e));
            }
            responseObserver.onError(statusException);
        }
    }

    @Override
    public void getUsers(com.google.protobuf.Empty request,
                         io.grpc.stub.StreamObserver<org.company.spacedrepetitiondata.grpc.AnalyticsProto.UsersResponse> responseObserver) {
        try {
            log.info("GetUsers method invoked");
            // Track metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementGetAnalyticsRequests(); // Using same metric as GetAnalytics for now
            }

            List<UserRecord> users = answerEventRepository.findAllUsers();
            org.company.spacedrepetitiondata.grpc.AnalyticsProto.UsersResponse.Builder responseBuilder =
                    org.company.spacedrepetitiondata.grpc.AnalyticsProto.UsersResponse.newBuilder();
            for (UserRecord user : users) {
                responseBuilder.addUsers(
                        org.company.spacedrepetitiondata.grpc.AnalyticsProto.User.newBuilder()
                                .setId(user.id())
                                .setName(user.name() != null ? user.name() : "")
                                .build()
                );
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("GetUsers processed successfully: returned {} users", users.size());
        } catch (Exception e) {
            // Track error metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementGetAnalyticsErrors();
            }
            log.error("Error processing GetUsers", e);
            StatusRuntimeException statusException;
            if (e instanceof StatusRuntimeException) {
                statusException = (StatusRuntimeException) e;
            } else if (e instanceof RuntimeException) {
                statusException = new StatusRuntimeException(io.grpc.Status.INTERNAL
                        .withDescription("Failed to retrieve users")
                        .withCause(e));
            } else {
                statusException = new StatusRuntimeException(io.grpc.Status.INTERNAL
                        .withDescription("Unexpected error")
                        .withCause(e));
            }
            responseObserver.onError(statusException);
        }
    }


    @Override
    public void streamAnalytics(org.company.spacedrepetitiondata.grpc.AnalyticsProto.StreamAnalyticsRequest request,
                          io.grpc.stub.StreamObserver<org.company.spacedrepetitiondata.grpc.AnalyticsProto.StreamAnalyticsResponse> responseObserver) {
        // Declare executor for cleanup (initialized later)
        ScheduledExecutorService executor = null;
        try {
            log.info("StreamAnalytics method invoked");
            log.info("StreamAnalytics received: user={}, startTime={}, endTime={}", 
                        request.hasUserId() ? request.getUserId() : "(all)", 
                        request.hasStartTime() ? request.getStartTime() : "(none)", 
                        request.hasEndTime() ? request.getEndTime() : "(none)");
            // Track metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementGetAnalyticsRequests();
            }

            // Parse optional filters
            final Long userId;
            if (request.hasUserId() && !request.getUserId().isEmpty()) {
                try {
                    userId = Long.parseLong(request.getUserId());
                } catch (NumberFormatException e) {
                    throw new StatusRuntimeException(Status.INVALID_ARGUMENT
                            .withDescription("Invalid user_id format: must be numeric")
                            .withCause(e));
                }
            } else {
                userId = null;
            }
            
            final Instant startTime;
            if (request.hasStartTime()) {
                startTime = Instant.ofEpochSecond(
                        request.getStartTime().getSeconds(),
                        request.getStartTime().getNanos());
            } else {
                startTime = null;
            }
            
            final Instant endTime;
            if (request.hasEndTime()) {
                endTime = Instant.ofEpochSecond(
                        request.getEndTime().getSeconds(),
                        request.getEndTime().getNanos());
            } else {
                endTime = null;
            }
            
            // Validate time range if both provided
            if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT
                        .withDescription("end_time must not be before start_time"));
            }
            
            // Query initial events matching filters
            List<AnswerEventRecord> initialEvents;
            if (userId != null) {
                log.info("Querying initial answer events for user {} in time range {} - {}", 
                            userId, startTime, endTime);
                initialEvents = answerEventRepository.findByUserIdAndTimeRange(userId, startTime, endTime);
            } else {
                log.info("Querying initial answer events for all users in time range {} - {}", 
                            startTime, endTime);
                initialEvents = answerEventRepository.findByTimeRange(startTime, endTime);
            }
            
            // Determine last sent timestamp
            AtomicReference<Instant> lastSentTimestampRef = new AtomicReference<>(
                startTime != null ? startTime : Instant.now()
            );
            if (!initialEvents.isEmpty()) {
                Instant maxTimestamp = initialEvents.stream()
                        .map(AnswerEventRecord::timestamp)
                        .max(Instant::compareTo)
                        .orElse(lastSentTimestampRef.get());
                lastSentTimestampRef.set(maxTimestamp);
            }
            
            // Stream initial events
            for (AnswerEventRecord record : initialEvents) {
                org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent event = 
                    AnswerEventRepository.toProto(record);
                org.company.spacedrepetitiondata.grpc.AnalyticsProto.StreamAnalyticsResponse response = 
                    org.company.spacedrepetitiondata.grpc.AnalyticsProto.StreamAnalyticsResponse.newBuilder()
                        .setEvent(event)
                        .build();
                try {
                    responseObserver.onNext(response);
                } catch (StatusRuntimeException e) {
                    if (e.getStatus().getCode() == io.grpc.Status.Code.CANCELLED) {
                        log.info("Client cancelled stream during initial event streaming");
                        return;
                    }
                    throw e; // rethrow other StatusRuntimeExceptions
                }
            }
            
            // Check if client already disconnected before scheduling polling
            if (Context.current().isCancelled()) {
                log.info("Client disconnected before polling started");
                return;
            }
            
            // Create scheduled executor for polling
            executor = Executors.newSingleThreadScheduledExecutor();
            final ScheduledExecutorService finalExecutor = executor; // final reference for lambda
            // Create final copies of filter variables for lambda
            final Long finalUserId = userId;
            final Instant finalStartTime = startTime;
            final Instant finalEndTime = endTime;
            
            // Store future for cancellation
            final java.util.concurrent.ScheduledFuture<?>[] pollingFutureRef = new java.util.concurrent.ScheduledFuture<?>[1];
            
            // Schedule periodic polling using configured interval
            pollingFutureRef[0] = executor.scheduleAtFixedRate(() -> {
                try {
                    // Check if client disconnected
                    if (Context.current().isCancelled()) {
                        log.info("Client disconnected, stopping polling");
                        pollingFutureRef[0].cancel(false); // Cancel scheduled future
                        finalExecutor.shutdown();
                        return;
                    }
                    
                    Instant currentLastSent = lastSentTimestampRef.get();
                    // Calculate adjusted start time for polling (max of last sent timestamp and original start time)
                    Instant adjustedStartTime = finalStartTime != null && finalStartTime.isAfter(currentLastSent) 
                            ? finalStartTime 
                            : currentLastSent;
                    
                    // Query new events
                    List<AnswerEventRecord> newEvents;
                    if (finalUserId != null) {
                        newEvents = answerEventRepository.findByUserIdAndTimeRange(
                                finalUserId, adjustedStartTime, finalEndTime);
                    } else {
                        newEvents = answerEventRepository.findByTimeRange(adjustedStartTime, finalEndTime);
                    }
                    
                    // Filter out events with timestamp <= lastSentTimestamp (should not happen due to adjusted start)
                    List<AnswerEventRecord> filteredNewEvents = newEvents.stream()
                            .filter(record -> record.timestamp().isAfter(currentLastSent))
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (!filteredNewEvents.isEmpty()) {
                        log.info("Polling found {} new events", filteredNewEvents.size());
                        // Send new events
                        for (AnswerEventRecord record : filteredNewEvents) {
                            org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent event = 
                                AnswerEventRepository.toProto(record);
                            org.company.spacedrepetitiondata.grpc.AnalyticsProto.StreamAnalyticsResponse
                                    response =
                                org.company.spacedrepetitiondata.grpc.AnalyticsProto.StreamAnalyticsResponse.
                                        newBuilder()
                                    .setEvent(event)
                                    .build();
                            responseObserver.onNext(response);
                        }
                        // Update last sent timestamp to max timestamp among new events
                        Instant maxTimestamp = filteredNewEvents.stream()
                                .map(AnswerEventRecord::timestamp)
                                .max(Instant::compareTo)
                                .orElse(currentLastSent);
                        lastSentTimestampRef.set(maxTimestamp);
                        log.debug("Updated last sent timestamp to {}", maxTimestamp);
                    } else {
                        log.debug("Polling found no new events");
                    }
                } catch (StatusRuntimeException e) {
                    if (e.getStatus().getCode() == io.grpc.Status.Code.CANCELLED) {
                        log.info("Client cancelled stream, stopping polling");
                        pollingFutureRef[0].cancel(false); // Cancel scheduled future
                        finalExecutor.shutdown();
                        return;
                    } else {
                        log.error("gRPC error during polling for new analytics events", e);
                        // Continue polling despite other gRPC errors
                    }
                } catch (Exception e) {
                    log.error("Error during polling for new analytics events", e);
                    // Continue polling despite errors
                }
            }, STREAMING_INITIAL_DELAY_SECONDS, STREAMING_POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);
            


            // Add context listener for immediate cleanup on client cancellation
            Context.current().addListener(context -> {
                if (context.isCancelled()) {
                    log.info("Context cancelled, shutting down polling executor");
                    pollingFutureRef[0].cancel(false); // Interrupt if running
                    finalExecutor.shutdown();
                }
            }, Runnable::run);

            // Set up a listener to clean up executor when client disconnects (already handled in polling task)
            // No need to call responseObserver.onCompleted() because streaming continues indefinitely.
            // The stream will remain open until client cancels.
            
        } catch (Exception e) {
            // Track error metrics if available
            if (metricsEndpoint != null) {
                metricsEndpoint.incrementGetAnalyticsErrors();
            }
            
            log.error("Error processing StreamAnalytics", e);
            
            // Clean up executor if it was created
            if (executor != null) {
                executor.shutdown();
            }
            StatusRuntimeException statusException;
            if (e instanceof StatusRuntimeException) {
                statusException = (StatusRuntimeException) e;
            } else if (e instanceof RuntimeException) {
                statusException = new StatusRuntimeException(io.grpc.Status.INTERNAL
                        .withDescription("Failed to stream analytics")
                        .withCause(e));
            } else {
                statusException = new StatusRuntimeException(io.grpc.Status.INTERNAL
                        .withDescription("Unexpected error")
                        .withCause(e));
            }
            responseObserver.onError(statusException);
        }
    }}