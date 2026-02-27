package org.company.spacedrepetitiondata.service;

import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.health.MetricsEndpoint;
import org.company.spacedrepetitiondata.repository.AnswerEventRecord;
import org.company.spacedrepetitiondata.repository.AnswerEventRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsServiceImpl using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private AnswerEventRepository answerEventRepository;

    @Mock
    private MetricsEndpoint metricsEndpoint;

    @Mock
    private StreamObserver<Empty> responseObserver;

    @Mock
    private StreamObserver<AnalyticsProto.AnalyticsResponse> analyticsResponseObserver;
    @Mock
    private StreamObserver<AnalyticsProto.StreamAnalyticsResponse> streamAnalyticsResponseObserver;

    @Captor
    private ArgumentCaptor<AnalyticsProto.StreamAnalyticsResponse> streamAnalyticsResponseCaptor;

    @Captor
    private ArgumentCaptor<Empty> emptyCaptor;

    @Captor
    private ArgumentCaptor<Throwable> throwableCaptor;

    @Captor
    private ArgumentCaptor<AnalyticsProto.AnalyticsResponse> analyticsResponseCaptor;

    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(answerEventRepository, metricsEndpoint);
    }

    @Test
    void recordAnswerEvent_validRequest_success() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("123")
                .setDeckId("456")
                .setCardId("789")
                .setQualityValue(4) // GOOD
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(1234567890L)
                        .setNanos(123456789)
                        .build())
                .build();

        AnswerEventRecord expectedRecord = AnswerEventRepository.fromProto(request);
        when(answerEventRepository.insert(any(AnswerEventRecord.class))).thenReturn(Optional.of(1L));

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(answerEventRepository).insert(expectedRecord);
        verify(responseObserver).onNext(any(Empty.class));
        verify(responseObserver).onCompleted();
        verifyNoMoreInteractions(responseObserver);
        verifyNoMoreInteractions(metricsEndpoint);
        verifyNoMoreInteractions(answerEventRepository);
    }

    @Test
    void recordAnswerEvent_missingUserId_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("")
                .setDeckId("456")
                .setCardId("789")
                .setQualityValue(4)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1234567890L).build())
                .build();

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(metricsEndpoint).incrementRecordAnswerEventErrors();
        verify(responseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("user_id is required"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void recordAnswerEvent_missingDeckId_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("123")
                .setDeckId("")
                .setCardId("789")
                .setQualityValue(4)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1234567890L).build())
                .build();

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(metricsEndpoint).incrementRecordAnswerEventErrors();
        verify(responseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("deck_id is required"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void recordAnswerEvent_missingCardId_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("123")
                .setDeckId("456")
                .setCardId("")
                .setQualityValue(4)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1234567890L).build())
                .build();

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(metricsEndpoint).incrementRecordAnswerEventErrors();
        verify(responseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("card_id is required"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void recordAnswerEvent_invalidQuality_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("123")
                .setDeckId("456")
                .setCardId("789")
                .setQualityValue(99) // invalid
                .setTimestamp(Timestamp.newBuilder().setSeconds(1234567890L).build())
                .build();

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(metricsEndpoint).incrementRecordAnswerEventErrors();
        verify(responseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("quality must be"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void recordAnswerEvent_missingTimestamp_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("123")
                .setDeckId("456")
                .setCardId("789")
                .setQualityValue(4)
                // timestamp not set
                .build();

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(metricsEndpoint).incrementRecordAnswerEventErrors();
        verify(responseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("timestamp is required"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void recordAnswerEvent_invalidIdFormat_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("not-a-number")
                .setDeckId("456")
                .setCardId("789")
                .setQualityValue(4)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1234567890L).build())
                .build();

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(metricsEndpoint).incrementRecordAnswerEventErrors();
        verify(responseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("Invalid ID format"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void recordAnswerEvent_repositoryThrowsRuntimeException_throwsInternal() {
        // Given
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("123")
                .setDeckId("456")
                .setCardId("789")
                .setQualityValue(4)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1234567890L).build())
                .build();

        RuntimeException dbError = new RuntimeException("Database connection failed");
        when(answerEventRepository.insert(any(AnswerEventRecord.class))).thenThrow(dbError);

        // When
        analyticsService.recordAnswerEvent(request, responseObserver);

        // Then
        verify(metricsEndpoint).incrementRecordAnswerEventRequests();
        verify(metricsEndpoint).incrementRecordAnswerEventErrors();
        verify(responseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INTERNAL.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("Failed to store answer event"));
        verify(answerEventRepository).insert(any(AnswerEventRecord.class));
    }

    @Test
    void recordAnswerEvent_metricsEndpointNull_doesNotTrackMetrics() {
        // Given
        AnalyticsServiceImpl serviceWithoutMetrics = new AnalyticsServiceImpl(answerEventRepository, null);
        AnalyticsProto.AnswerEvent request = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId("123")
                .setDeckId("456")
                .setCardId("789")
                .setQualityValue(4)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1234567890L).build())
                .build();
        when(answerEventRepository.insert(any(AnswerEventRecord.class))).thenReturn(Optional.of(1L));

        // When
        serviceWithoutMetrics.recordAnswerEvent(request, responseObserver);

        // Then
        verify(answerEventRepository).insert(any(AnswerEventRecord.class));
        verify(responseObserver).onNext(any(Empty.class));
        verify(responseObserver).onCompleted();
        verifyNoInteractions(metricsEndpoint);
    }

    @Test
    void getAnalytics_validRequest_success() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(1000)
                        .setNanos(0)
                        .build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(2000)
                        .setNanos(0)
                        .build())
                .build();

        List<AnswerEventRecord> mockEvents = List.of(
                new AnswerEventRecord(123L, 456L, 789L, 4, Instant.ofEpochSecond(1500))
        );
        when(answerEventRepository.findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenReturn(mockEvents);
        when(answerEventRepository.countByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(answerEventRepository).findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        verify(answerEventRepository).countByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        verify(analyticsResponseObserver).onNext(analyticsResponseCaptor.capture());
        verify(analyticsResponseObserver).onCompleted();
        verifyNoMoreInteractions(analyticsResponseObserver);
        verifyNoMoreInteractions(metricsEndpoint);
        verifyNoMoreInteractions(answerEventRepository);

        AnalyticsProto.AnalyticsResponse response = analyticsResponseCaptor.getValue();
        assertNotNull(response);
        assertEquals(1, response.getEventsCount());
        assertEquals(1, response.getTotalCount());
        AnalyticsProto.AnswerEvent event = response.getEvents(0);
        assertEquals("123", event.getUserId());
        assertEquals("456", event.getDeckId());
        assertEquals("789", event.getCardId());
        assertEquals(4, event.getQualityValue());
        assertEquals(1500, event.getTimestamp().getSeconds());
    }

    @Test
    void getAnalytics_emptyUserId_returnsAllUsers() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        List<AnswerEventRecord> mockEvents = List.of(
                new AnswerEventRecord(123L, 456L, 789L, 4, Instant.ofEpochSecond(1500)),
                new AnswerEventRecord(456L, 789L, 1011L, 5, Instant.ofEpochSecond(1800))
        );
        when(answerEventRepository.findByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(mockEvents);
        when(answerEventRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(2L);

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(answerEventRepository).findByTimeRange(any(Instant.class), any(Instant.class));
        verify(answerEventRepository).countByTimeRange(any(Instant.class), any(Instant.class));
        verify(analyticsResponseObserver).onNext(analyticsResponseCaptor.capture());
        verify(analyticsResponseObserver).onCompleted();
        verifyNoMoreInteractions(analyticsResponseObserver);
        verifyNoMoreInteractions(metricsEndpoint);
        verifyNoMoreInteractions(answerEventRepository);

        AnalyticsProto.AnalyticsResponse response = analyticsResponseCaptor.getValue();
        assertNotNull(response);
        assertEquals(2, response.getEventsCount());
        assertEquals(2, response.getTotalCount());
        // Verify user IDs are present in response
        assertEquals("123", response.getEvents(0).getUserId());
        assertEquals("456", response.getEvents(1).getUserId());
    }

    @Test
    void getAnalytics_missingStartTime_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("123")
                // start_time not set
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(metricsEndpoint).incrementGetAnalyticsErrors();
        verify(analyticsResponseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("start_time is required"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void getAnalytics_missingEndTime_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                // end_time not set
                .build();

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(metricsEndpoint).incrementGetAnalyticsErrors();
        verify(analyticsResponseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("end_time is required"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void getAnalytics_endTimeBeforeStartTime_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .build();

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(metricsEndpoint).incrementGetAnalyticsErrors();
        verify(analyticsResponseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("end_time must not be before start_time"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void getAnalytics_invalidUserIdFormat_throwsInvalidArgument() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("not-a-number")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(metricsEndpoint).incrementGetAnalyticsErrors();
        verify(analyticsResponseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("Invalid user_id format"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void getAnalytics_noEventsFound_returnsEmpty() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        when(answerEventRepository.findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(answerEventRepository.countByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(answerEventRepository).findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        verify(answerEventRepository).countByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        verify(analyticsResponseObserver).onNext(analyticsResponseCaptor.capture());
        verify(analyticsResponseObserver).onCompleted();
        verifyNoMoreInteractions(analyticsResponseObserver);
        verifyNoMoreInteractions(metricsEndpoint);
        verifyNoMoreInteractions(answerEventRepository);

        AnalyticsProto.AnalyticsResponse response = analyticsResponseCaptor.getValue();
        assertNotNull(response);
        assertEquals(0, response.getEventsCount());
        assertEquals(0, response.getTotalCount());
    }

    @Test
    void getAnalytics_repositoryThrowsRuntimeException_throwsInternal() {
        // Given
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        RuntimeException dbError = new RuntimeException("Database connection failed");
        when(answerEventRepository.findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenThrow(dbError);

        // When
        analyticsService.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(metricsEndpoint).incrementGetAnalyticsErrors();
        verify(analyticsResponseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INTERNAL.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("Failed to retrieve analytics"));
        verify(answerEventRepository).findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        verifyNoMoreInteractions(answerEventRepository);
    }

    @Test
    void getAnalytics_metricsEndpointNull_doesNotTrackMetrics() {
        // Given
        AnalyticsServiceImpl serviceWithoutMetrics = new AnalyticsServiceImpl(answerEventRepository, null);
        AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        when(answerEventRepository.findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(answerEventRepository.countByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        // When
        serviceWithoutMetrics.getAnalytics(request, analyticsResponseObserver);

        // Then
        verify(answerEventRepository).findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        verify(answerEventRepository).countByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        verify(analyticsResponseObserver).onNext(any(AnalyticsProto.AnalyticsResponse.class));
        verify(analyticsResponseObserver).onCompleted();
        verifyNoInteractions(metricsEndpoint);
    }
    @Test
    void streamAnalytics_validRequest_streamsInitialEvents() {
        // Given
        AnalyticsProto.StreamAnalyticsRequest request = AnalyticsProto.StreamAnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        List<AnswerEventRecord> mockEvents = List.of(
                new AnswerEventRecord(123L, 456L, 789L, 4, Instant.ofEpochSecond(1500)),
                new AnswerEventRecord(123L, 456L, 790L, 5, Instant.ofEpochSecond(1600))
        );
        when(answerEventRepository.findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class)))
                .thenReturn(mockEvents);

        // Setup the stream observer to throw CANCELLED after receiving initial events
        // This will cause the polling task to shut down the executor
        doNothing()
                .doThrow(new StatusRuntimeException(Status.CANCELLED))
                .when(streamAnalyticsResponseObserver).onNext(any(AnalyticsProto.StreamAnalyticsResponse.class));

        // When
        analyticsService.streamAnalytics(request, streamAnalyticsResponseObserver);

        // Then - verify initial events are streamed
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(answerEventRepository).findByUserIdAndTimeRange(eq(123L), any(Instant.class), any(Instant.class));
        // Verify two events were sent
        verify(streamAnalyticsResponseObserver, times(2)).onNext(any(AnalyticsProto.StreamAnalyticsResponse.class));
        // onCompleted() should NOT be called for streaming
        verify(streamAnalyticsResponseObserver, never()).onCompleted();
        // The executor will be shut down due to CANCELLED exception, but we can't verify that easily
        // Instead, we just ensure no errors were propagated to onError
        verify(streamAnalyticsResponseObserver, never()).onError(any(Throwable.class));
    }

    @Test
    void streamAnalytics_emptyUserId_returnsAllUsers() {
        // Given
        AnalyticsProto.StreamAnalyticsRequest request = AnalyticsProto.StreamAnalyticsRequest.newBuilder()
                .setUserId("")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        List<AnswerEventRecord> mockEvents = List.of(
                new AnswerEventRecord(123L, 456L, 789L, 4, Instant.ofEpochSecond(1500))
        );
        when(answerEventRepository.findByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(mockEvents);

        // Setup the stream observer to throw CANCELLED after receiving initial events
        doNothing()
                .doThrow(new StatusRuntimeException(Status.CANCELLED))
                .when(streamAnalyticsResponseObserver).onNext(any(AnalyticsProto.StreamAnalyticsResponse.class));

        // When
        analyticsService.streamAnalytics(request, streamAnalyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(answerEventRepository).findByTimeRange(any(Instant.class), any(Instant.class));
        verify(streamAnalyticsResponseObserver, times(1)).onNext(any(AnalyticsProto.StreamAnalyticsResponse.class));
        verify(streamAnalyticsResponseObserver, never()).onCompleted();
        verify(streamAnalyticsResponseObserver, never()).onError(any(Throwable.class));
    }

    @Test
    void streamAnalytics_invalidUserIdFormat_throwsInvalidArgument() {
        // Given
        AnalyticsProto.StreamAnalyticsRequest request = AnalyticsProto.StreamAnalyticsRequest.newBuilder()
                .setUserId("not-a-number")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        // When
        analyticsService.streamAnalytics(request, streamAnalyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(metricsEndpoint).incrementGetAnalyticsErrors();
        verify(streamAnalyticsResponseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("Invalid user_id format"));
        verifyNoInteractions(answerEventRepository);
    }

    @Test
    void streamAnalytics_endTimeBeforeStartTime_throwsInvalidArgument() {
        // Given
        AnalyticsProto.StreamAnalyticsRequest request = AnalyticsProto.StreamAnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(2000).build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(1000).build())
                .build();

        // When
        analyticsService.streamAnalytics(request, streamAnalyticsResponseObserver);

        // Then
        verify(metricsEndpoint).incrementGetAnalyticsRequests();
        verify(metricsEndpoint).incrementGetAnalyticsErrors();
        verify(streamAnalyticsResponseObserver).onError(throwableCaptor.capture());
        Throwable thrown = throwableCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, thrown);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusRuntimeException) thrown).getStatus().getCode());
        assertTrue(((StatusRuntimeException) thrown).getStatus().getDescription().contains("end_time must not be before start_time"));
        verifyNoInteractions(answerEventRepository);
    }

}