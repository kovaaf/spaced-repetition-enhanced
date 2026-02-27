package org.company.spacedrepetitionbot.client.analytics;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnalyticsServiceClient}.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceClientTest {

    @Mock
    private AnalyticsServiceGrpc.AnalyticsServiceBlockingStub analyticsStub;

    @InjectMocks
    private AnalyticsServiceClient analyticsServiceClient;

    @Test
    void shouldRecordAnswerEventSuccessfully() {
        // Given
        String userId = "user123";
        String deckId = "deck456";
        String cardId = "card789";
        Quality quality = Quality.GOOD;
        Instant timestamp = Instant.now();
        Empty expectedResponse = Empty.getDefaultInstance();

        when(analyticsStub.recordAnswerEvent(any(AnalyticsProto.AnswerEvent.class)))
                .thenReturn(expectedResponse);

        // When
        analyticsServiceClient.recordAnswerEvent(userId, deckId, cardId, quality, timestamp);

        // Then
        ArgumentCaptor<AnalyticsProto.AnswerEvent> captor = ArgumentCaptor.forClass(AnalyticsProto.AnswerEvent.class);
        verify(analyticsStub).recordAnswerEvent(captor.capture());
        AnalyticsProto.AnswerEvent capturedEvent = captor.getValue();

        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(deckId, capturedEvent.getDeckId());
        assertEquals(cardId, capturedEvent.getCardId());
        assertEquals(AnalyticsProto.Quality.GOOD, capturedEvent.getQuality());
        assertTrue(capturedEvent.hasTimestamp());
        assertEquals(timestamp.getEpochSecond(), capturedEvent.getTimestamp().getSeconds());
        assertEquals(timestamp.getNano(), capturedEvent.getTimestamp().getNanos());
    }

    @Test
    void shouldRecordAnswerEventWithoutTimestamp() {
        // Given
        String userId = "user123";
        String deckId = "deck456";
        String cardId = "card789";
        Quality quality = Quality.EASY;
        Instant timestamp = null;
        Empty expectedResponse = Empty.getDefaultInstance();

        when(analyticsStub.recordAnswerEvent(any(AnalyticsProto.AnswerEvent.class)))
                .thenReturn(expectedResponse);

        // When
        analyticsServiceClient.recordAnswerEvent(userId, deckId, cardId, quality, timestamp);

        // Then
        ArgumentCaptor<AnalyticsProto.AnswerEvent> captor = ArgumentCaptor.forClass(AnalyticsProto.AnswerEvent.class);
        verify(analyticsStub).recordAnswerEvent(captor.capture());
        AnalyticsProto.AnswerEvent capturedEvent = captor.getValue();

        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(deckId, capturedEvent.getDeckId());
        assertEquals(cardId, capturedEvent.getCardId());
        assertEquals(AnalyticsProto.Quality.EASY, capturedEvent.getQuality());
        assertFalse(capturedEvent.hasTimestamp());
    }

    @Test
    void shouldMapAllQualityValuesCorrectly() {
        // Given
        String userId = "user123";
        String deckId = "deck456";
        String cardId = "card789";
        Instant timestamp = Instant.now();
        Empty expectedResponse = Empty.getDefaultInstance();

        // Use lenient stubbing to avoid unnecessary stubbing exception
        lenient().when(analyticsStub.recordAnswerEvent(any(AnalyticsProto.AnswerEvent.class)))
                .thenReturn(expectedResponse);

        // Test each quality
        for (Quality quality : Quality.values()) {
            // When
            analyticsServiceClient.recordAnswerEvent(userId, deckId, cardId, quality, timestamp);
        }

        // Then
        ArgumentCaptor<AnalyticsProto.AnswerEvent> captor = ArgumentCaptor.forClass(AnalyticsProto.AnswerEvent.class);
        verify(analyticsStub, times(Quality.values().length)).recordAnswerEvent(captor.capture());
        var capturedEvents = captor.getAllValues();
        
        assertEquals(Quality.values().length, capturedEvents.size());
        for (int i = 0; i < Quality.values().length; i++) {
            Quality expectedQuality = Quality.values()[i];
            AnalyticsProto.Quality expectedProtoQuality = AnalyticsProto.Quality.forNumber(expectedQuality.getQuality());
            assertEquals(expectedProtoQuality, capturedEvents.get(i).getQuality(),
                    "Quality mapping mismatch for " + expectedQuality);
        }
    }

    @Test
    void shouldThrowRuntimeExceptionWhenGrpcCallFails() {
        // Given
        String userId = "user123";
        String deckId = "deck456";
        String cardId = "card789";
        Quality quality = Quality.HARD;
        Instant timestamp = Instant.now();

        StatusRuntimeException grpcException = new StatusRuntimeException(Status.INTERNAL
                .withDescription("Internal server error"));
        when(analyticsStub.recordAnswerEvent(any(AnalyticsProto.AnswerEvent.class)))
                .thenThrow(grpcException);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> analyticsServiceClient.recordAnswerEvent(userId, deckId, cardId, quality, timestamp));
        assertEquals("Failed to record answer event", exception.getMessage());
        assertSame(grpcException, exception.getCause());
    }
}