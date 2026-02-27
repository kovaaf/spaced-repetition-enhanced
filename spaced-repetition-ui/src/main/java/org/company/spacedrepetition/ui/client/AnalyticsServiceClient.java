package org.company.spacedrepetition.ui.client;

import com.google.protobuf.Timestamp;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

import java.util.function.Consumer;
/**
 * Plain Java gRPC client for the Analytics service.
 * Connects to the data service for retrieving statistics.
 */
public class AnalyticsServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsServiceClient.class);
    // Retry configuration constants
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int MAX_STREAMING_RETRY_ATTEMPTS = 5;
    private static final long BASE_RETRY_DELAY_MS = 1000L;
    private static final long MAX_RETRY_DELAY_MS = 60000L;
    
    // Exponential backoff configuration
    private static final double JITTER_FACTOR = 0.2;
    
    // Default configuration
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9091;
    
    private final ManagedChannel channel;
    private final AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;
    private final AnalyticsServiceGrpc.AnalyticsServiceStub asyncStub;
    
    /**
     * Constructs a client connecting to the given host and port.
     */
    public AnalyticsServiceClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                // Conservative keepalive settings to avoid 'too_many_pings' error
                .keepAliveTime(300, TimeUnit.SECONDS)      // Send keepalive ping every 5 minutes (not 30s)
                .keepAliveTimeout(30, TimeUnit.SECONDS)    // Wait 30 seconds for keepalive acknowledgment
                .keepAliveWithoutCalls(true)               // Keep connection alive even without active RPCs
                .idleTimeout(24, TimeUnit.HOURS)          // Extended idle timeout for long-lived connections
                .maxInboundMessageSize(10 * 1024 * 1024)   // 10MB max message size
                .build());
    }
    
    /**
     * Constructs a client using an existing channel.
     */
    public AnalyticsServiceClient(ManagedChannel channel) {
        this.channel = channel;
        this.blockingStub = AnalyticsServiceGrpc.newBlockingStub(channel);
        this.asyncStub = AnalyticsServiceGrpc.newStub(channel);
    }

    /**
     * Stream observer that automatically reconnects with exponential backoff
     * when the stream encounters errors.
     */
    private class ReconnectingStreamObserver
            implements io.grpc.stub.StreamObserver<AnalyticsProto.StreamAnalyticsResponse> {
        private final AnalyticsProto.StreamAnalyticsRequest request;
        private final Consumer<AnalyticsProto.AnswerEvent> eventConsumer;
        private final Runnable completionCallback;
        private final Consumer<Throwable> errorCallback;
        private final int maxAttempts;
        private final long baseDelayMs;
        private final long maxDelayMs;
        private final ScheduledExecutorService scheduler;
        private final Random random;
        
        private final AtomicInteger currentAttempt;
        private volatile ScheduledFuture<?> reconnectFuture;
        private volatile boolean shutdownRequested;
        
        /**
         * Creates a new reconnecting stream observer.
         * @param maxAttempts maximum number of reconnection attempts (default 5)
         * @param baseDelayMs base delay for exponential backoff in milliseconds (default 1000)
         * @param maxDelayMs maximum delay between reconnections in milliseconds (default 60000)
         */
        ReconnectingStreamObserver(AnalyticsProto.StreamAnalyticsRequest request,
                                          Consumer<AnalyticsProto.AnswerEvent> eventConsumer,
                                          Runnable completionCallback,
                                          Consumer<Throwable> errorCallback,
                                          int maxAttempts,
                                          long baseDelayMs,
                                          long maxDelayMs) {
            this.request = request;
            this.eventConsumer = eventConsumer;
            this.completionCallback = completionCallback;
            this.errorCallback = errorCallback;
            this.maxAttempts = maxAttempts;
            this.baseDelayMs = baseDelayMs;
            this.maxDelayMs = maxDelayMs;
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.random = new Random();
            this.currentAttempt = new AtomicInteger(0);
            this.reconnectFuture = null;
            this.shutdownRequested = false;
        }

        /**
         * Determines if an error is retryable based on gRPC status code.
         * Only UNAVAILABLE and DEADLINE_EXCEEDED are considered retryable.
         */
        private boolean isRetryableError(Throwable t) {
            if (t instanceof StatusRuntimeException) {
                Status.Code code = ((StatusRuntimeException) t).getStatus().getCode();
                return code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED;
            }
            // Non-gRPC exceptions are not retryable
            return false;
        }
        /**
         * Starts the stream.
         */

        public void start() {
            LOG.info("Starting streaming with reconnection support (max attempts: {})", maxAttempts);
            startStream();
        }
        
        /**
         * Shuts down the scheduler and cancels any pending reconnections.
         */
        public void shutdownScheduler() {
            shutdownRequested = true;
            if (reconnectFuture != null && !reconnectFuture.isDone()) {
                reconnectFuture.cancel(false);
            }
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        /**
         * Initiates a new stream connection.
         */
        private void startStream() {
            if (shutdownRequested) {
                LOG.debug("Shutdown requested, not starting new stream");
                return;
            }
            int attempt = currentAttempt.incrementAndGet();
            LOG.info("Starting stream attempt {}/{}", attempt, maxAttempts);
            
            io.grpc.stub.StreamObserver<AnalyticsProto.StreamAnalyticsResponse> observer = new io.grpc.stub.StreamObserver<>() {
                @Override
                public void onNext(AnalyticsProto.StreamAnalyticsResponse response) {
                    AnalyticsProto.AnswerEvent event = response.getEvent();
                    LOG.debug(
                            "Received streaming event: user={}, deck={}, card={}, quality={}",
                            event.getUserId(),
                            event.getDeckId(),
                            event.getCardId(),
                            event.getQuality());
                    eventConsumer.accept(event);
                }

                @Override
                public void onError(Throwable t) {
                    LOG.warn("Stream error on attempt {}/{}", attempt, maxAttempts, t);
                    if (shutdownRequested) {
                        return;
                    }
                    if (!isRetryableError(t)) {
                        LOG.error("Non-retryable error, terminating stream: {}", t.getMessage());
                        errorCallback.accept(t);
                        shutdownScheduler();
                        return;
                    }
                    if (attempt >= maxAttempts) {
                        LOG.error("Max reconnection attempts ({}) reached, " + "falling back to polling", maxAttempts);
                        errorCallback.accept(new RuntimeException("Streaming permanently unavailable after " +
                                maxAttempts +
                                " attempts, falling back to polling"));
                        shutdownScheduler();
                        return;
                    }
                    scheduleReconnection();
                }

                @Override
                public void onCompleted() {
                    LOG.info("Stream completed successfully on attempt {}/{}", attempt, maxAttempts);
                    completionCallback.run();
                    shutdownScheduler();
                }
            };
            
            asyncStub.streamAnalytics(request, observer);
        }
        
        /**
         * Schedules a reconnection attempt with exponential backoff and jitter.
         */
        private void scheduleReconnection() {
            if (shutdownRequested) {
                return;
            }
            long delay = calculateBackoffWithJitter(currentAttempt.get());
            LOG.info("Scheduling reconnection attempt {}/{} in {} ms", currentAttempt.get(), maxAttempts, delay);
            reconnectFuture = scheduler.schedule(() -> {
                if (!shutdownRequested) {
                    startStream();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
        
        /**
         * Calculates exponential backoff with jitter.
         * Formula: baseDelayMs * (1L << (attempt - 1)) + random jitter up to 20% of the exponential backoff.
         * Capped at maxDelayMs.
         * @param attempt the current attempt number (1-based)
         * @return backoff delay in milliseconds
         */
        private long calculateBackoffWithJitter(int attempt) {
            if (attempt <= 1) {
                return baseDelayMs;
            }
            // Exponential backoff: baseDelayMs * 2^(attempt-1)
            long exponentialBackoff = baseDelayMs * (1L << (attempt - 1));
            // Add random jitter up to 20% of exponential backoff
            long jitter = (long) (random.nextDouble() * JITTER_FACTOR * exponentialBackoff);
            long total = exponentialBackoff + jitter;
            // Cap at maxDelayMs
            return Math.min(total, maxDelayMs);
        }
        
        @Override
        public void onNext(AnalyticsProto.StreamAnalyticsResponse response) {
            // This method is never called because we delegate to inner observer
            throw new UnsupportedOperationException("ReconnectingStreamObserver delegates to inner observer");
        }
        
        @Override
        public void onError(Throwable t) {
            // This method is never called because we delegate to inner observer
            throw new UnsupportedOperationException("ReconnectingStreamObserver delegates to inner observer");
        }
        
        @Override
        public void onCompleted() {
            // This method is never called because we delegate to inner observer
            throw new UnsupportedOperationException("ReconnectingStreamObserver delegates to inner observer");
        }
    }

    /**
     * Package-private constructor for testing, directly injects a stub.
     * Package-private constructor for testing with both stubs.
     */
    AnalyticsServiceClient(AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub,
                           AnalyticsServiceGrpc.AnalyticsServiceStub asyncStub) {
        this.channel = null;
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    /**
     * Package-private constructor for testing, directly injects a stub.
     */
    AnalyticsServiceClient(AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub) {
        this(blockingStub, null);
    }
    
    /**
     * Retrieves analytics events for the specified user and time range.
     * Implements retry logic for UNAVAILABLE and DEADLINE_EXCEEDED statuses (max 3 attempts).
     *
     * @param userId the user ID
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return analytics response containing events and total count
     * @throws RuntimeException if the gRPC call fails after all retries
     */
    public AnalyticsProto.AnalyticsResponse getAnalytics(String userId, Instant startTime, Instant endTime) {
        AnalyticsProto.AnalyticsRequest request = buildAnalyticsRequest(userId, startTime, endTime);
        
        int maxAttempts = MAX_RETRY_ATTEMPTS;
        int attempt = 0;
        long backoffMillis = BASE_RETRY_DELAY_MS; // initial backoff 1 second
        
        while (attempt < maxAttempts) {
            attempt++;
            try {
                LOG.debug("Attempt {} to get analytics for user {}", attempt, userId);
                AnalyticsProto.AnalyticsResponse response = blockingStub.getAnalytics(request);
                LOG.info("Successfully retrieved analytics for user {}, {} events", 
                        userId, response.getTotalCount());
                return response;
            } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();
                if ((code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) 
                        && attempt < maxAttempts) {
                    LOG.warn("gRPC call failed with {} (attempt {}), retrying after {} ms", 
                            code, attempt, backoffMillis, e);
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry", ie);
                    }
                    // exponential backoff
                    backoffMillis *= 2;
                } else {
                    LOG.error("Failed to get analytics for user {} after {} attempts", 
                            userId, attempt, e);
                    throw new RuntimeException("Failed to retrieve analytics", e);
                }
            }
        }
        // Should not reach here
        throw new RuntimeException("Failed to retrieve analytics after " + maxAttempts + " attempts");
    }

    /**
     * Retrieves all users from the data service.
     * Implements retry logic for UNAVAILABLE and DEADLINE_EXCEEDED statuses (max 3 attempts).
     *
     * @return users response containing list of users (id, name)
     * @throws RuntimeException if the gRPC call fails after all retries
     */
    public AnalyticsProto.UsersResponse getUsers() {
        int maxAttempts = MAX_RETRY_ATTEMPTS;
        int attempt = 0;
        long backoffMillis = BASE_RETRY_DELAY_MS; // initial backoff 1 second
        
        while (attempt < maxAttempts) {
            attempt++;
            try {
                LOG.debug("Attempt {} to get users", attempt);
                AnalyticsProto.UsersResponse response = blockingStub.getUsers(Empty.getDefaultInstance());
                LOG.info("Successfully retrieved {} users", response.getUsersCount());
                return response;
            } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();
                if ((code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) 
                        && attempt < maxAttempts) {
                    LOG.warn("gRPC call failed with {} (attempt {}), retrying after {} ms", 
                            code, attempt, backoffMillis, e);
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry", ie);
                    }
                    // exponential backoff
                    backoffMillis *= 2;
                } else {
                    LOG.error("Failed to get users after {} attempts", attempt, e);
                    throw new RuntimeException("Failed to retrieve users", e);
                }
            }
        }
        // Should not reach here
        throw new RuntimeException("Failed to retrieve users after " + maxAttempts + " attempts");
    }
    
    /**
     * Shuts down the channel, releasing resources.
     */
    public void shutdown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    /**
     * Creates a client using configuration from application.properties.
     * Expects property "data.service.url" in format "host:port" (default localhost:9091).
     *
     * @return configured client
     * @throws RuntimeException if configuration cannot be loaded or parsed
     */
    public static AnalyticsServiceClient createFromConfig() {
        Properties props = new Properties();
        try (InputStream input = AnalyticsServiceClient.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                LOG.warn("application.properties not found, using default localhost:9091");
                return new AnalyticsServiceClient("localhost", 9091);
            }
            props.load(input);
        } catch (IOException e) {
            LOG.warn("Failed to load application.properties, using default localhost:9091", e);
            return new AnalyticsServiceClient("localhost", 9091);
        }
        
        String url = props.getProperty("data.service.url", "localhost:9091");
        String[] parts = url.split(":");
        if (parts.length != 2) {
            LOG.warn("Invalid data.service.url format: {}, using default localhost:9091", url);
            return new AnalyticsServiceClient("localhost", 9091);
        }
        String host = parts[0].trim();
        int port;
        try {
            port = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            LOG.warn("Invalid port in data.service.url: {}, using default 9091", parts[1], e);
            port = 9091;
        }
        LOG.info("Creating AnalyticsServiceClient for {}:{}", host, port);
        return new AnalyticsServiceClient(host, port);
    }
    
    private AnalyticsProto.AnalyticsRequest buildAnalyticsRequest(String userId, 
            Instant startTime, Instant endTime) {
        AnalyticsProto.AnalyticsRequest.Builder builder = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId(userId);
        if (startTime != null) {
            builder.setStartTime(instantToTimestamp(startTime));
        }
        if (endTime != null) {
            builder.setEndTime(instantToTimestamp(endTime));
        }
        return builder.build();
    }
    
    private Timestamp instantToTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Streams analytics events in real-time.
     *
     * @param userId the user ID to filter by (null for all users)
     * @param startTime start of time range (inclusive, null for no start limit)
     * @param endTime end of time range (inclusive, null for no end limit)
     * @param eventConsumer callback for each received event
     * @param completionCallback callback when stream completes successfully
     * @param errorCallback callback when stream encounters an error
     */
    public void streamAnalytics(String userId, Instant startTime, Instant endTime,
                               Consumer<AnalyticsProto.AnswerEvent> eventConsumer,
                               Runnable completionCallback,
                               Consumer<Throwable> errorCallback) {
        AnalyticsProto.StreamAnalyticsRequest.Builder builder = AnalyticsProto.StreamAnalyticsRequest.newBuilder();
        if (userId != null) {
            builder.setUserId(userId);
        }
        if (startTime != null) {
            builder.setStartTime(instantToTimestamp(startTime));
        }
        if (endTime != null) {
            builder.setEndTime(instantToTimestamp(endTime));
        }
        
        AnalyticsProto.StreamAnalyticsRequest request = builder.build();
        
        // Create reconnecting stream observer with exponential backoff
        // Defaults: max 5 attempts, base delay 1 second, max delay 1 minute
        ReconnectingStreamObserver observer = new ReconnectingStreamObserver(
                request, eventConsumer, completionCallback, errorCallback,
                MAX_STREAMING_RETRY_ATTEMPTS, BASE_RETRY_DELAY_MS, MAX_RETRY_DELAY_MS);
        observer.start();
    }
}