package org.company.spacedrepetition.ui.integration;

import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * QA verification for Task 6: Update MainFrame to display names instead of IDs.
 * Implements the two QA scenarios from the plan:
 * 1. Verify name display in table when name fields are present
 * 2. Verify fallback to IDs when names missing
 */
class Task6NameDisplayTest extends UITestBase {

    @Test
    void tableShouldDisplayNamesWhenAvailable() {
        // Given: mock data response with name fields
        AnalyticsProto.AnalyticsResponse response = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(2)
                .addEvents(createAnswerEventWithNames(
                        "123", "John Doe",
                        "456", "Java Basics",
                        "789", "What is Java?",
                        AnalyticsProto.Quality.GOOD,
                        Instant.parse("2026-02-01T10:00:00Z")))
                .addEvents(createAnswerEventWithNames(
                        "999", "Alice",
                        "888", "Algorithms",
                        "777", "Binary Search",
                        AnalyticsProto.Quality.EASY,
                        Instant.parse("2026-02-01T11:00:00Z")))
                .build();

        when(mockClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(response);

        // When: trigger data refresh
        triggerRefreshData();

        // Wait for SwingWorker to complete
        window.robot().waitForIdle();

        // Then: verify table displays names, not IDs
        // Access the JTable via reflection to check row values
        try {
            var tableField = org.company.spacedrepetition.ui.frame.MainFrame.class.getDeclaredField("statisticsTable");
            tableField.setAccessible(true);
            var table = (javax.swing.JTable) tableField.get(mainFrame);

            // Wait a bit more for table model to update
            Thread.sleep(100);
            window.robot().waitForIdle();

            // Verify table has 2 rows
            assertThat(table.getRowCount()).isEqualTo(2);

            // Verify specific values in the table - names should be displayed
            var model = table.getModel();
            // Row 0: John Doe, Java Basics, What is Java?
            assertThat(model.getValueAt(0, 0)).isEqualTo("John Doe");
            assertThat(model.getValueAt(0, 1)).isEqualTo("Java Basics");
            assertThat(model.getValueAt(0, 2)).isEqualTo("What is Java?");
            assertThat(model.getValueAt(0, 3)).isEqualTo("Good (4)");
            // Date formatted as "yyyy-MM-dd HH:mm"
            assertThat(model.getValueAt(0, 4)).asString().startsWith("2026-02-01");

            // Row 1: Alice, Algorithms, Binary Search
            assertThat(model.getValueAt(1, 0)).isEqualTo("Alice");
            assertThat(model.getValueAt(1, 1)).isEqualTo("Algorithms");
            assertThat(model.getValueAt(1, 2)).isEqualTo("Binary Search");
            assertThat(model.getValueAt(1, 3)).isEqualTo("Easy (5)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify table contents", e);
        }
    }

    @Test
    void tableShouldFallbackToIdsWhenNamesMissing() {
        // Given: mock data response WITHOUT name fields (null/empty)
        AnalyticsProto.AnalyticsResponse response = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(2)
                .addEvents(createAnswerEventWithoutNames(
                        "123", "456", "789",
                        AnalyticsProto.Quality.GOOD,
                        Instant.parse("2026-02-01T10:00:00Z")))
                .addEvents(createAnswerEventWithoutNames(
                        "999", "888", "777",
                        AnalyticsProto.Quality.EASY,
                        Instant.parse("2026-02-01T11:00:00Z")))
                .build();

        when(mockClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(response);

        // When: trigger data refresh
        triggerRefreshData();

        // Wait for SwingWorker to complete
        window.robot().waitForIdle();

        // Then: verify table displays IDs as fallback
        try {
            var tableField = org.company.spacedrepetition.ui.frame.MainFrame.class.getDeclaredField("statisticsTable");
            tableField.setAccessible(true);
            var table = (javax.swing.JTable) tableField.get(mainFrame);

            Thread.sleep(100);
            window.robot().waitForIdle();

            assertThat(table.getRowCount()).isEqualTo(2);

            var model = table.getModel();
            // Row 0: IDs should be shown
            assertThat(model.getValueAt(0, 0)).isEqualTo("123");
            assertThat(model.getValueAt(0, 1)).isEqualTo("456");
            assertThat(model.getValueAt(0, 2)).isEqualTo("789");
            assertThat(model.getValueAt(0, 3)).isEqualTo("Good (4)");

            // Row 1
            assertThat(model.getValueAt(1, 0)).isEqualTo("999");
            assertThat(model.getValueAt(1, 1)).isEqualTo("888");
            assertThat(model.getValueAt(1, 2)).isEqualTo("777");
            assertThat(model.getValueAt(1, 3)).isEqualTo("Easy (5)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify table fallback", e);
        }
    }

    // Helper methods
    private AnalyticsProto.AnswerEvent createAnswerEventWithNames(
            String userId, String userName,
            String deckId, String deckName,
            String cardId, String cardTitle,
            AnalyticsProto.Quality quality, Instant timestamp) {
        return AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId(userId)
                .setUserName(userName)
                .setDeckId(deckId)
                .setDeckName(deckName)
                .setCardId(cardId)
                .setCardTitle(cardTitle)
                .setQuality(quality)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
    }

    private AnalyticsProto.AnswerEvent createAnswerEventWithoutNames(
            String userId, String deckId, String cardId,
            AnalyticsProto.Quality quality, Instant timestamp) {
        return AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId(userId)
                .setDeckId(deckId)
                .setCardId(cardId)
                .setQuality(quality)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
    }

    private void triggerRefreshData() {
        // Use reflection to call refreshData on MainFrame (copied from MainFrameIntegrationTest)
        try {
            var method = org.company.spacedrepetition.ui.frame.MainFrame.class.getDeclaredMethod("refreshData");
            method.setAccessible(true);
            method.invoke(mainFrame);
        } catch (Exception e) {
            throw new RuntimeException("Failed to trigger refreshData", e);
        }
    }
}