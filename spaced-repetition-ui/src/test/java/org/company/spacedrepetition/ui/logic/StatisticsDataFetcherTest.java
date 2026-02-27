package org.company.spacedrepetition.ui.logic;

import org.company.spacedrepetition.ui.client.AnalyticsServiceClient;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StatisticsDataFetcher}.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsDataFetcherTest {

    @Mock
    private AnalyticsServiceClient analyticsClient;

    @Test
    void fetchData_shouldCallClientWithCorrectParameters() throws Exception {
        // Given
        StatisticsDataFetcher fetcher = new StatisticsDataFetcher(analyticsClient);
        String userId = "user123";
        Instant startTime = Instant.parse("2026-02-01T00:00:00Z");
        Instant endTime = Instant.parse("2026-02-28T23:59:59Z");
        
        AnalyticsProto.AnalyticsResponse mockResponse = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(3)
                .build();
        
        when(analyticsClient.getAnalytics(userId, startTime, endTime))
                .thenReturn(mockResponse);
        
        // Use latch to wait for async completion
        CountDownLatch latch = new CountDownLatch(1);
        AnalyticsProto.AnalyticsResponse[] capturedResponse = new AnalyticsProto.AnalyticsResponse[1];
        Exception[] capturedException = new Exception[1];
        
        // When
        fetcher.fetchData(userId, startTime, endTime,
                response -> {
                    capturedResponse[0] = response;
                    latch.countDown();
                },
                exception -> {
                    capturedException[0] = exception;
                    latch.countDown();
                });
        
        // Wait for async completion (max 2 seconds)
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");
        
        // Then
        verify(analyticsClient).getAnalytics(userId, startTime, endTime);
        assertNotNull(capturedResponse[0]);
        assertEquals(mockResponse.getTotalCount(), capturedResponse[0].getTotalCount());
        assertNull(capturedException[0]);
    }

    @Test
    void fetchData_shouldInvokeErrorCallbackOnClientException() throws Exception {
        // Given
        StatisticsDataFetcher fetcher = new StatisticsDataFetcher(analyticsClient);
        String userId = "user123";
        Instant startTime = Instant.now().minusSeconds(3600);
        Instant endTime = Instant.now();
        
        RuntimeException expectedException = new RuntimeException("Network error");
        when(analyticsClient.getAnalytics(userId, startTime, endTime))
                .thenThrow(expectedException);
        
        CountDownLatch latch = new CountDownLatch(1);
        AnalyticsProto.AnalyticsResponse[] capturedResponse = new AnalyticsProto.AnalyticsResponse[1];
        Exception[] capturedException = new Exception[1];
        
        // When
        fetcher.fetchData(userId, startTime, endTime,
                response -> {
                    capturedResponse[0] = response;
                    latch.countDown();
                },
                exception -> {
                    capturedException[0] = exception;
                    latch.countDown();
                });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");
        
        // Then
        verify(analyticsClient).getAnalytics(userId, startTime, endTime);
        assertNull(capturedResponse[0]);
        assertNotNull(capturedException[0]);
        assertEquals(expectedException, capturedException[0].getCause());
    }

    @Test
    void periodToTimeRange_lastWeek_shouldReturnCorrectRange() {
        // When
        Instant[] range = StatisticsDataFetcher.periodToTimeRange("Last Week");
        
        // Then
        assertNotNull(range);
        assertEquals(2, range.length);
        Instant start = range[0];
        Instant end = range[1];
        
        // End should be close to now (allow small delta)
        Instant now = Instant.now();
        assertTrue(end.plusSeconds(5).isAfter(now) && end.minusSeconds(5).isBefore(now),
                "End time should be within 5 seconds of current time");
        
        // Start should be 7 days before end
        assertEquals(7 * 24 * 3600, end.getEpochSecond() - start.getEpochSecond(), 10); // 10 second tolerance
    }

    @Test
    void periodToTimeRange_lastMonth_shouldReturnCorrectRange() {
        // When
        Instant[] range = StatisticsDataFetcher.periodToTimeRange("Last Month");
        
        // Then
        assertNotNull(range);
        assertEquals(2, range.length);
        Instant start = range[0];
        Instant end = range[1];
        
        // Start should be approximately 30 days before end
        assertEquals(30L * 24 * 3600, end.getEpochSecond() - start.getEpochSecond(), 10);
    }

    @Test
    void periodToTimeRange_lastYear_shouldReturnCorrectRange() {
        // When
        Instant[] range = StatisticsDataFetcher.periodToTimeRange("Last Year");
        
        // Then
        assertNotNull(range);
        assertEquals(2, range.length);
        Instant start = range[0];
        Instant end = range[1];
        
        // Start should be 365 days before end
        assertEquals(365L * 24 * 3600, end.getEpochSecond() - start.getEpochSecond(), 10);
    }

    @Test
    void periodToTimeRange_allTime_shouldReturnEpochToNow() {
        // When
        Instant[] range = StatisticsDataFetcher.periodToTimeRange("All Time");
        
        // Then
        assertNotNull(range);
        assertEquals(2, range.length);
        assertEquals(Instant.EPOCH, range[0]);
        // end should be close to now (within 5 seconds)
        Instant now = Instant.now();
        Instant end = range[1];
        assertTrue(end.plusSeconds(5).isAfter(now) && end.minusSeconds(5).isBefore(now),
                "End time should be within 5 seconds of current time");
    }

    @Test
    void periodToTimeRange_unknownPeriod_shouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> StatisticsDataFetcher.periodToTimeRange("Unknown Period"));
        assertTrue(exception.getMessage().contains("Unknown period"));
    }

    @Test
    void userSelectionToUserId_allUsers_shouldReturnEmptyString() {
        // When
        String result = StatisticsDataFetcher.userSelectionToUserId("All Users");
        
        // Then
        assertEquals("", result);
    }

    @Test
    void userSelectionToUserId_nullSelection_shouldReturnEmptyString() {
        // When
        String result = StatisticsDataFetcher.userSelectionToUserId(null);
        
        // Then
        assertEquals("", result);
    }

    @Test
    void userSelectionToUserId_userN_shouldReturnNumericPart() {
        // When
        String result = StatisticsDataFetcher.userSelectionToUserId("User 42");
        
        // Then
        assertEquals("42", result);
    }

    @Test
    void userSelectionToUserId_alreadyNumericId_shouldReturnSame() {
        // When
        String result = StatisticsDataFetcher.userSelectionToUserId("123");
        
        // Then
        assertEquals("123", result);
    }

    @Test
    void userSelectionToUserId_malformedUserPattern_shouldReturnOriginal() {
        // When
        String result = StatisticsDataFetcher.userSelectionToUserId("Not User Pattern");
        
        // Then
        assertEquals("Not User Pattern", result);
    }

    @Test
    void fetchUsers_shouldCallClientAndReturnUsers() throws Exception {
        // Given
        StatisticsDataFetcher fetcher = new StatisticsDataFetcher(analyticsClient);
        
        AnalyticsProto.UsersResponse mockResponse = AnalyticsProto.UsersResponse.newBuilder()
                .addUsers(AnalyticsProto.User.newBuilder()
                    .setId(123)
                    .setName("John Doe")
                    .build())
                .addUsers(AnalyticsProto.User.newBuilder()
                    .setId(456)
                    .setName("Jane Smith")
                    .build())
                .build();
        
        when(analyticsClient.getUsers())
                .thenReturn(mockResponse);
        
        // Use latch to wait for async completion
        CountDownLatch latch = new CountDownLatch(1);
        AnalyticsProto.UsersResponse[] capturedResponse = new AnalyticsProto.UsersResponse[1];
        Exception[] capturedException = new Exception[1];
        
        // When
        fetcher.fetchUsers(
                response -> {
                    capturedResponse[0] = response;
                    latch.countDown();
                },
                exception -> {
                    capturedException[0] = exception;
                    latch.countDown();
                });
        
        // Wait for async completion (max 2 seconds)
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");
        
        // Then
        verify(analyticsClient).getUsers();
        assertNotNull(capturedResponse[0]);
        assertEquals(2, capturedResponse[0].getUsersCount());
        assertEquals(123, capturedResponse[0].getUsers(0).getId());
        assertEquals("John Doe", capturedResponse[0].getUsers(0).getName());
        assertNull(capturedException[0]);
    }

    @Test
    void fetchUsers_shouldInvokeErrorCallbackOnClientException() throws Exception {
        // Given
        StatisticsDataFetcher fetcher = new StatisticsDataFetcher(analyticsClient);
        
        RuntimeException expectedException = new RuntimeException("Network error");
        when(analyticsClient.getUsers())
                .thenThrow(expectedException);
        
        CountDownLatch latch = new CountDownLatch(1);
        AnalyticsProto.UsersResponse[] capturedResponse = new AnalyticsProto.UsersResponse[1];
        Exception[] capturedException = new Exception[1];
        
        // When
        fetcher.fetchUsers(
                response -> {
                    capturedResponse[0] = response;
                    latch.countDown();
                },
                exception -> {
                    capturedException[0] = exception;
                    latch.countDown();
                });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");
        
        // Then
        verify(analyticsClient).getUsers();
        assertNull(capturedResponse[0]);
        assertNotNull(capturedException[0]);
        assertEquals(expectedException, capturedException[0].getCause());
    }
}