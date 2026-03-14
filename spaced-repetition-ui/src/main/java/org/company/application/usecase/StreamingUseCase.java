package org.company.application.usecase;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.StreamingListener;
import org.company.domain.StreamingService;

import java.time.Instant;

@Slf4j
public class StreamingUseCase {
    @Setter
    private StreamingService streamingService;

    public StreamingUseCase(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    public void startStreaming(Instant startTime, StreamingListener listener) {
        log.info("Starting streaming from {}", startTime);
        streamingService.startStreaming(startTime, listener);
    }

    public void stopStreaming() {
        log.info("Stopping streaming");
        streamingService.stopStreaming();
    }
}