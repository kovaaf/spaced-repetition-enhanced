package org.company.ui.application.usecase;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.ui.application.port.input.StreamingExecutor;
import org.company.ui.application.port.output.grpc.StreamingListener;
import org.company.ui.application.port.output.grpc.StreamingService;

import java.time.Instant;

/**
 * Use case for managing the real‑time event stream.
 * Wraps a {@link StreamingService} and provides a simplified interface.
 */
@Slf4j
public class StreamingExecutorUseCase implements StreamingExecutor {
    @Setter
    private StreamingService streamingService;

    public StreamingExecutorUseCase(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    /**
     * Starts streaming events from a given start time.
     *
     * @param startTime the lower bound (exclusive) for event timestamps
     * @param listener  callback for received events
     */
    public void startStreaming(Instant startTime, StreamingListener listener) {
        log.info("Starting streaming from {}", startTime);
        streamingService.startStreaming(startTime, listener);
    }

    /**
     * Stops the currently active stream.
     */
    public void stopStreaming() {
        log.info("Stopping streaming");
        streamingService.stopStreaming();
    }
}