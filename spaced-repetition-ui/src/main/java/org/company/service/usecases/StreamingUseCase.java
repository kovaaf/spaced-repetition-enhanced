package org.company.service.usecases;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.presentation.presenter.StreamingListener;
import org.company.service.dao.StreamingService;

import java.time.Instant;

/**
 * Use case for managing the real‑time event stream.
 * Wraps a {@link StreamingService} and provides a simplified interface.
 */
@Slf4j
public class StreamingUseCase {
    @Setter
    private StreamingService streamingService;

    public StreamingUseCase(StreamingService streamingService) {
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