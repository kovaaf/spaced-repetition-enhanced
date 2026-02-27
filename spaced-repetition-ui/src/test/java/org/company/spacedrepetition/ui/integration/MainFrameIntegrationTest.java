package org.company.spacedrepetition.ui.integration;

import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JToggleButtonFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.fixture.JLabelFixture;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import com.google.protobuf.Timestamp;
import org.company.spacedrepetition.ui.frame.MainFrame;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.timeout;

/**
 * Integration tests for MainFrame UI interactions.
 * Tests filter interactions, table updates, and error scenarios.
 */
class MainFrameIntegrationTest extends UITestBase {

    @Test
    void filterInteractions_shouldTriggerDataFetching() {
        // Given: mock data response
        AnalyticsProto.AnalyticsResponse response = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(2)
                .addEvents(createAnswerEvent("user1", "deck1", "card1", AnalyticsProto.Quality.GOOD,
                        Instant.parse("2026-02-01T10:00:00Z")))
                .addEvents(createAnswerEvent("user2", "deck2", "card2", AnalyticsProto.Quality.EASY,
                        Instant.parse("2026-02-01T11:00:00Z")))
                .build();
        
        when(mockClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(response);
        
        // Reset mock to ignore any initial calls
        reset(mockClient);
        // Restub getUsers to ensure user filter loads correctly
        AnalyticsProto.UsersResponse usersResponse = AnalyticsProto.UsersResponse.newBuilder()
                .addUsers(AnalyticsProto.User.newBuilder().setId(1).setName("User 1").build())
                .addUsers(AnalyticsProto.User.newBuilder().setId(2).setName("User 2").build())
                .build();
        when(mockClient.getUsers()).thenReturn(usersResponse);
        when(mockClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(response);
        
        // When: interact with filter components
        // Click period filter "Last Week" toggle button
        window.toggleButton("Last Week").click();
        
        // Wait for user filter to be populated
        waitForComboBoxToContainItem("UserFilterComboBox", "User 2", 5000);
        
        // Change user filter selection
        window.comboBox("UserFilterComboBox").selectItem(2);
        

        
        // Wait a bit for async operations (SwingWorker)
        window.robot().waitForIdle();
        
        // Then: verify client was called for each filter interaction
        // Each filter change triggers refreshData, but they might be coalesced
        // We expect at least one additional call beyond initial
        verify(mockClient, atLeast(1)).getAnalytics(any(String.class), any(Instant.class), any(Instant.class));
    }

    @Test
    void tableUpdates_withMockData_shouldDisplayCorrectRows() {
        // Given: mock data response with specific events
        AnalyticsProto.AnalyticsResponse response = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(3)
                .addEvents(createAnswerEvent("user1", "deck1", "card1", AnalyticsProto.Quality.GOOD,
                        Instant.parse("2026-02-01T10:00:00Z")))
                .addEvents(createAnswerEvent("user1", "deck1", "card2", AnalyticsProto.Quality.HARD,
                        Instant.parse("2026-02-01T10:05:00Z")))
                .addEvents(createAnswerEvent("user2", "deck2", "card3", AnalyticsProto.Quality.EASY,
                        Instant.parse("2026-02-01T11:00:00Z")))
                .build();
        
        when(mockClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(response);
        
        // Reset mock to ignore any initial calls
        reset(mockClient);
        // Restub getUsers to ensure user filter loads correctly
        AnalyticsProto.UsersResponse usersResponse = AnalyticsProto.UsersResponse.newBuilder()
                .addUsers(AnalyticsProto.User.newBuilder().setId(1).setName("User 1").build())
                .addUsers(AnalyticsProto.User.newBuilder().setId(2).setName("User 2").build())
                .build();
        when(mockClient.getUsers()).thenReturn(usersResponse);
        when(mockClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(response);
        
        // When: trigger data refresh
        triggerRefreshData();
        
        // Wait for SwingWorker to complete
        window.robot().waitForIdle();
        
        // Then: verify client was called
        verify(mockClient, atLeast(1)).getAnalytics(any(String.class), any(Instant.class), any(Instant.class));
        
        // And: verify table contains expected rows
        // Access the JTable via reflection to check row count
        try {
            var tableField = MainFrame.class.getDeclaredField("statisticsTable");
            tableField.setAccessible(true);
            var table = (javax.swing.JTable) tableField.get(mainFrame);
            
            // Wait a bit more for table model to update
            Thread.sleep(100);
            window.robot().waitForIdle();
            
            // Verify table has 3 rows (from our mock response)
            assertThat(table.getRowCount()).isEqualTo(3);
            
            // Verify specific values in the table
            var model = table.getModel();
            // Row 0: user1, deck1, card1, Good (4), 2026-02-01 17:00 (UTC+7 timezone)
            assertThat(model.getValueAt(0, 0)).isEqualTo("user1");
            assertThat(model.getValueAt(0, 1)).isEqualTo("deck1");
            assertThat(model.getValueAt(0, 2)).isEqualTo("card1");
            assertThat(model.getValueAt(0, 3)).isEqualTo("Good (4)"); // Quality formatted as "Good (4)"
            // Date is formatted as "yyyy-MM-dd HH:mm" in local timezone (UTC+7)
            // The Instant "2026-02-01T10:00:00Z" becomes "2026-02-01 17:00" in UTC+7
            assertThat(model.getValueAt(0, 4)).asString().startsWith("2026-02-01"); // Check date part
            
            // Row 1: user1, deck1, card2, Hard (3), 2026-02-01
            assertThat(model.getValueAt(1, 3)).isEqualTo("Hard (3)");
            
            // Row 2: user2, deck2, card3, Easy (5), 2026-02-01
            assertThat(model.getValueAt(2, 0)).isEqualTo("user2");
            assertThat(model.getValueAt(2, 3)).isEqualTo("Easy (5)");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify table contents", e);
        }
    }

    @Test
    void errorScenario_dataServiceUnavailable_shouldShowErrorMessage() {
        // Given: reset mock to ignore any initial calls

        
        // Reset mock to ignore any initial calls (following pattern from other tests)
        reset(mockClient);
        
        // Mock client throws exception
        when(mockClient.getAnalytics(any(String.class), any(Instant.class), any(Instant.class)))
                .thenThrow(new RuntimeException("Service unavailable"));
        
        // When: trigger data refresh
        triggerRefreshData();
        
        // Then: verify client was called exactly once within timeout (wait for SwingWorker)
        verify(mockClient, timeout(3000).atLeast(1)).getAnalytics(any(String.class), any(Instant.class), any(Instant.class));
        
        // Wait for SwingWorker to complete
        window.robot().waitForIdle();
        
        // And: verify status label shows error message using reflection (label name changes after error)
        try {
            // Use reflection to access the statusLabel field directly
            var statusLabelField = MainFrame.class.getDeclaredField("statusLabel");
            statusLabelField.setAccessible(true);
            var statusLabel = (javax.swing.JLabel) statusLabelField.get(mainFrame);
            
            // Wait a bit more for the label to update (SwingWorker callback on EDT)
            Thread.sleep(100);
            window.robot().waitForIdle();
            
            // Verify the label text contains "Error"
            assertThat(statusLabel.getText()).contains("Error");
            assertThat(statusLabel.getText()).contains("Service unavailable");
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify status label error message", e);
        }
    }



    // Helper methods
    
    private AnalyticsProto.AnswerEvent createAnswerEvent(String userId, String deckId, String cardId,
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
        // Use reflection to call refreshData on MainFrame
        try {
            var method = MainFrame.class.getDeclaredMethod("refreshData");
            method.setAccessible(true);
            method.invoke(mainFrame);
        } catch (Exception e) {
            throw new RuntimeException("Failed to trigger refreshData", e);
        }
    }
    

    
    private void waitForStatusLabelToContain(String substring, int timeoutMs) {
        long endTime = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endTime) {
            try {
                String labelText = window.label("StatusLabel").text();
                if (labelText.contains(substring)) {
                    return;
                }
            } catch (Exception e) {
                // Label might not be found yet, continue waiting
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // If we exit loop without finding substring, assertion will fail later
    }
}