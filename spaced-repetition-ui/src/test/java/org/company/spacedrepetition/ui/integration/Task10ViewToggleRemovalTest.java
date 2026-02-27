package org.company.spacedrepetition.ui.integration;

// JTableFixture not used

import org.junit.jupiter.api.Test;

import org.company.spacedrepetition.ui.frame.MainFrame;
import javax.swing.*;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA test for Task 10: Verify ViewToggle component removal.
 */
public class Task10ViewToggleRemovalTest extends UITestBase {

    @Test
    void viewToggleComponent_shouldNotExist() {
        // Verify no component with name containing "ViewToggle" exists
        Component[] components = mainFrame.getContentPane().getComponents();
        for (Component comp : components) {
            assertThat(comp.getName()).isNotEqualTo("ViewToggle");
            // Also check class name
            assertThat(comp.getClass().getSimpleName()).isNotEqualTo("ViewToggle");
        }
        // Verify no ViewToggle class in classpath (by checking we can't load it)
        try {
            Class.forName("org.company.spacedrepetition.ui.components.filters.ViewToggle");
            // If we reach here, class exists - fail
            throw new AssertionError("ViewToggle class should not exist");
        } catch (ClassNotFoundException e) {
            // Expected - class not found
        }
    }

    @Test
    void onlyTableViewAvailable_tableShouldBeVisibleAndFunctional() {
        // Verify table exists and is visible using reflection (same as MainFrameIntegrationTest)
        try {
            var tableField = MainFrame.class.getDeclaredField("statisticsTable");
            tableField.setAccessible(true);
            var table = (javax.swing.JTable) tableField.get(mainFrame);
            
            assertThat(table.isVisible()).isTrue();
            assertThat(table.getRowCount()).isGreaterThanOrEqualTo(0);
            assertThat(table.getColumnCount()).isGreaterThanOrEqualTo(4); // User, Deck, Card, Quality, Date
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify table", e);
        }
        // Verify no chart toggle buttons exist
        boolean foundChartToggle = false;
        for (Component comp : mainFrame.getContentPane().getComponents()) {
            if (comp instanceof javax.swing.AbstractButton) {
                String text = ((javax.swing.AbstractButton) comp).getText();
                if (text != null && (text.equals("Chart") || text.equals("Table"))) {
                    foundChartToggle = true;
                    break;
                }
            }
        }
        assertThat(foundChartToggle).isFalse();
}
}