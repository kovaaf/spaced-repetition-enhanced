package org.company.ui.application.port.input;

import org.company.ui.application.port.output.grpc.StreamingListener;

import java.time.Instant;

public interface StreamingExecutor {
    void startStreaming(Instant startTime, StreamingListener listener);
    void stopStreaming();
}