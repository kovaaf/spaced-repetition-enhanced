package org.company.domain;

import java.time.Instant;

public interface StreamingService {
    void startStreaming(Instant startTime, StreamingListener listener);
    void stopStreaming();
}
