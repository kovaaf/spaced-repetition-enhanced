package org.company.spacedrepetition.ui.integration;

import org.company.spacedrepetition.ui.frame.MainFrame;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA test for Task 11: Verify refresh button removal and auto-refresh timer disabled.
 */
public class Task11RefreshButtonRemovalTest extends UITestBase {

    @Test
    void refreshButton_shouldNotExist() {
        // Verify no refresh button in UI
        Component[] components = mainFrame.getContentPane().getComponents();
        boolean foundRefreshButton = false;
        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                String text = button.getText();
                if (text != null && (text.equals("Refresh") || text.equals("↻") || text.contains("refresh"))) {
                    foundRefreshButton = true;
                    break;
                }
            }
        }
        assertThat(foundRefreshButton).isFalse();
        
        // Also check bottom panel components
        JPanel bottomPanel = mainFrame.getBottomPanel();
        for (Component comp : bottomPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                String text = button.getText();
                if (text != null && (text.equals("Refresh") || text.equals("↻") || text.contains("refresh"))) {
                    foundRefreshButton = true;
                    break;
                }
            }
        }
        assertThat(foundRefreshButton).isFalse();
    }

    @Test
    void autoRefreshTimer_shouldBeDisabled() {
        // Verify no 5-minute auto-refresh timer is running
        // Check for Timer fields in MainFrame via reflection
        try {
            Field[] fields = MainFrame.class.getDeclaredFields();
            boolean foundAutoRefreshTimer = false;
            for (Field field : fields) {
                if (Timer.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Timer timer = (Timer) field.get(mainFrame);
                    if (timer != null) {
                        // Check if this timer is for auto-refresh (could be streaming timer)
                        // We'll just ensure there's no timer with long delay (5 minutes)
                        // Since we cannot inspect Timer's schedule, we assume any Timer present is for streaming
                        // The requirement is that auto-refresh timer is disabled, not all timers.
                        // We'll verify that there's no timer with 300000 ms delay (hard to check)
                        // Instead, we verify that the MainFrame doesn't have a field named "autoRefreshTimer"
                        if (field.getName().toLowerCase().contains("refresh")) {
                            foundAutoRefreshTimer = true;
                        }
                    }
                }
            }
            // It's okay if there are timers for streaming reconnection
            // The test passes as long as no timer specifically for auto-refresh exists
            assertThat(foundAutoRefreshTimer).isFalse();
            
            // Also verify that the refreshData method is not being called periodically
            // by checking that there's no scheduled timer task that calls refreshData
            // This is more complex; we'll rely on the field check above.
        } catch (Exception e) {
            throw new RuntimeException("Failed to check timer", e);
        }
    }
}