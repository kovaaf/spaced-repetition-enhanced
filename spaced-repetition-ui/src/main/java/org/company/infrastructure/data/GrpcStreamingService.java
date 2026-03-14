package org.company.infrastructure.data;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.AnswerEvent;
import org.company.domain.StreamingListener;
import org.company.domain.StreamingService;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;

import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class GrpcStreamingService implements StreamingService {
    private final AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;
    private final AtomicBoolean streamingActive = new AtomicBoolean(false);
    private Thread streamingThread;

    public GrpcStreamingService(GrpcDataService grpcDataService) {
        this.blockingStub = grpcDataService.getBlockingStub();
    }

    @Override
    public void startStreaming(Instant startTime, StreamingListener listener) {
        if (streamingActive.get()) {
            log.warn("Streaming already active, stopping previous");
            stopStreaming();
        }

        AnalyticsProto.StreamAnalyticsRequest.Builder builder = AnalyticsProto.StreamAnalyticsRequest.newBuilder();
        if (startTime != null) {
            builder.setStartTime(com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(startTime.getEpochSecond())
                    .setNanos(startTime.getNano()));
        }
        AnalyticsProto.StreamAnalyticsRequest request = builder.build();

        Iterator<AnalyticsProto.StreamAnalyticsResponse> iterator = blockingStub.streamAnalytics(request);

        streamingActive.set(true);
        streamingThread = new Thread(() -> {
            try {
                while (streamingActive.get() && iterator.hasNext()) {
                    AnalyticsProto.StreamAnalyticsResponse response = iterator.next();
                    AnswerEvent event = toAnswerEvent(response.getEvent());
                    listener.onEvent(event);
                }
                if (streamingActive.get()) {
                    listener.onCompleted();
                }
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == io.grpc.Status.Code.CANCELLED && !streamingActive.get()) {
                    listener.onCompleted();
                } else {
                    listener.onError(e);
                }
            } catch (Exception e) {
                listener.onError(e);
            } finally {
                streamingActive.set(false);
            }
        }, "gRPC-streaming-thread");
        streamingThread.setDaemon(true);
        streamingThread.start();
    }

    @Override
    public void stopStreaming() {
        streamingActive.set(false);
        if (streamingThread != null) {
            streamingThread.interrupt();
            streamingThread = null;
        }
    }

    private AnswerEvent toAnswerEvent(AnalyticsProto.AnswerEvent proto) {
        Instant timestamp = Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos());
        return new AnswerEvent(
                proto.getUserId(),
                proto.hasUserName() ? proto.getUserName() : null,
                proto.getDeckId(),
                proto.hasDeckName() ? proto.getDeckName() : null,
                proto.getCardId(),
                proto.hasCardTitle() ? proto.getCardTitle() : null,
                proto.getQualityValue(),
                timestamp);
    }
}