package org.company.spacedrepetition.ui.logic;

import org.company.spacedrepetition.ui.client.AnalyticsServiceClient;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Integration test for NameCache integration with StatisticsDataFetcher.
 * Verifies that name fields from analytics responses are cached and reused.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsDataFetcherCacheIntegrationTest {

    @Mock
    private AnalyticsServiceClient analyticsClient;

    private StatisticsDataFetcher fetcher;
    private NameCache cache;

    @BeforeEach
    void setUp() {
        fetcher = new StatisticsDataFetcher(analyticsClient);
        cache = NameCache.getInstance();
        cache.clear(); // Ensure clean cache before each test
    }

    @Test
    void fetchData_shouldPopulateNameCache() throws Exception {
        // Given: Mock response with name fields
        AnalyticsProto.AnalyticsResponse mockResponse = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(2)
                .addEvents(AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("123")
                        .setUserName("John Doe")
                        .setDeckId("456")
                        .setDeckName("Java Basics")
                        .setCardId("789")
                        .setCardTitle("What is Java?")
                        .setQuality(AnalyticsProto.Quality.GOOD) // GOOD
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .build())
                .addEvents(AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("999")
                        .setUserName("Alice")
                        .setDeckId("888")
                        .setDeckName("Algorithms")
                        .setCardId("777")
                        .setCardTitle("Binary Search")
                        .setQuality(AnalyticsProto.Quality.EASY) // EASY
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .build())
                .build();

        when(analyticsClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(mockResponse);

        CountDownLatch latch = new CountDownLatch(1);
        AnalyticsProto.AnalyticsResponse[] capturedResponse = new AnalyticsProto.AnalyticsResponse[1];

        // When: Fetch data
        fetcher.fetchData("", Instant.now().minusSeconds(3600), Instant.now(),
                response -> {
                    capturedResponse[0] = response;
                    latch.countDown();
                },
                exception -> fail("Should not call error callback"));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");

        // Then: Cache should contain names
        assertEquals("John Doe", cache.get("123"), "User name should be cached");
        assertEquals("Java Basics", cache.get("456"), "Deck name should be cached");
        assertEquals("What is Java?", cache.get("789"), "Card title should be cached");
        assertEquals("Alice", cache.get("999"), "Second user name should be cached");
        assertEquals("Algorithms", cache.get("888"), "Second deck name should be cached");
        assertEquals("Binary Search", cache.get("777"), "Second card title should be cached");
    }

    @Test
    void fetchData_shouldUseCachedNamesForSubsequentRequests() throws Exception {
        // Given: Pre-populate cache with some names
        cache.put("123", "Cached User");
        cache.put("456", "Cached Deck");
        cache.put("789", "Cached Card");

        // Mock response with empty name fields (simulating missing names in response)
        AnalyticsProto.AnalyticsResponse mockResponse = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(1)
                .addEvents(AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("123")
                        .setDeckId("456")
                        .setCardId("789")
                        .setQuality(AnalyticsProto.Quality.GOOD)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        // No name fields set (simulating missing optional fields)
                        .build())
                .build();

        when(analyticsClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(mockResponse);

        CountDownLatch latch = new CountDownLatch(1);
        AnalyticsProto.AnalyticsResponse[] capturedResponse = new AnalyticsProto.AnalyticsResponse[1];

        // When: Fetch data
        fetcher.fetchData("", Instant.now().minusSeconds(3600), Instant.now(),
                response -> {
                    capturedResponse[0] = response;
                    latch.countDown();
                },
                exception -> fail("Should not call error callback"));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");

        // Then: Cache entries should still exist (not cleared by empty response)
        assertEquals("Cached User", cache.get("123"), "Cached user name should still be present");
        assertEquals("Cached Deck", cache.get("456"), "Cached deck name should still be present");
        assertEquals("Cached Card", cache.get("789"), "Cached card title should still be present");
    }

    @Test
    void fetchData_shouldIgnoreNullOrEmptyNames() throws Exception {
        // Given: Mock response with empty string name (should not be cached)
        AnalyticsProto.AnalyticsResponse mockResponse = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(1)
                .addEvents(AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("111")
                        .setUserName("")  // Empty string
                        .setDeckId("222")
                        .setDeckName("   ")  // Whitespace only
                        .setCardId("333")
                        // No card title set (optional field missing)
                        .setQuality(AnalyticsProto.Quality.HARD)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .build())
                .build();

        when(analyticsClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(mockResponse);

        CountDownLatch latch = new CountDownLatch(1);
        AnalyticsProto.AnalyticsResponse[] capturedResponse = new AnalyticsProto.AnalyticsResponse[1];

        // When: Fetch data
        fetcher.fetchData("", Instant.now().minusSeconds(3600), Instant.now(),
                response -> {
                    capturedResponse[0] = response;
                    latch.countDown();
                },
                exception -> fail("Should not call error callback"));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");

        // Then: Cache should NOT contain entries for empty/whitespace names
        assertNull(cache.get("111"), "Empty user name should not be cached");
        assertNull(cache.get("222"), "Whitespace deck name should not be cached");
        assertNull(cache.get("333"), "Missing card title should not be cached");
    }
}