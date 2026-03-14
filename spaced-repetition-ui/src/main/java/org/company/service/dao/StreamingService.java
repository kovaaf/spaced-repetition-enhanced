package org.company.service.dao;

import org.company.presentation.presenter.StreamingListener;

import java.time.Instant;

/**
 * Service responsible for real‑time streaming of answer events.
 * Events are pushed to the provided listener as they arrive.
 */
public interface StreamingService {
    /**
     * Starts streaming events with timestamps after the given start time.
     * If a stream is already active, it is stopped before starting the new one.
     *
     * @param startTime the lower bound (exclusive) for event timestamps
     * @param listener  callback for received events, errors and completion
     */
    void startStreaming(Instant startTime, StreamingListener listener);

    /**
     * Stops the currently active streaming session (if any).
     * The streaming thread is interrupted and the listener will not receive further events.
     */
    void stopStreaming();
}
