package org.company.spacedrepetitiondata.service.grpc;

import io.grpc.stub.StreamObserver;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;

/**
 * Strategy for streaming analytics events.
 */
public interface StreamingStrategy {
    void stream(AnalyticsProto.StreamAnalyticsRequest request,
            StreamObserver<AnalyticsProto.StreamAnalyticsResponse> responseObserver);
}