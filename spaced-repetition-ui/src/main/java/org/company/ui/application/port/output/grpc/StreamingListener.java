package org.company.ui.application.port.output.grpc;

import org.company.ui.domain.entity.AnswerEvent;

/**
 * Callback interface for receiving streaming events.
 * Implementations are called from the streaming thread, so they should
 * delegate UI updates to the Event Dispatch Thread if needed.
 */
public interface StreamingListener {
    /**
     * Called when a new answer event arrives from the stream.
     *
     * @param event the incoming event
     */
    void onEvent(AnswerEvent event);

    /**
     * Called when an error occurs during streaming.
     *
     * @param error the error that occurred
     */
    void onError(Throwable error);

    /**
     * Called when the stream completes normally (server closes the stream).
     */
    void onCompleted();
}
