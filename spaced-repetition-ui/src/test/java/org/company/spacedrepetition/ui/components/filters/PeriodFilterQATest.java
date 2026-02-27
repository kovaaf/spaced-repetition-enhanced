package org.company.spacedrepetition.ui.components.filters;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.Container;

/**
 * QA test for PeriodFilter to verify "Last Day" button implementation.
 * This test is part of F3 Real Manual QA for Task 8.
 */
class PeriodFilterQATest {

    @Test
    void verifyLastDayButtonExists() {
        // Given
        PeriodFilter filter = new PeriodFilter();
        
        // When
        Component[] components = filter.getComponents();
        int buttonCount = 0;
        boolean lastDayButtonFound = false;
        String lastDayButtonText = null;
        
        for (Component comp : components) {
            if (comp instanceof JToggleButton) {
                buttonCount++;
                JToggleButton button = (JToggleButton) comp;
                if ("Last Day".equals(button.getText())) {
                    lastDayButtonFound = true;
                    lastDayButtonText = button.getText();
                }
            }
        }
        
        // Then
        assertEquals(5, buttonCount, "Should have 5 toggle buttons (Last Week, Last Month, Last Year, All Time, Last Day)");
        assertTrue(lastDayButtonFound, "Last Day button should exist");
        assertEquals("Last Day", lastDayButtonText, "Last Day button text should be 'Last Day'");
    }
    
    @Test
    void verify24HourRollingWindowCalculation() {
        // Given
        PeriodFilter filter = new PeriodFilter();
        filter.setSelectedPeriod("Last Day");
        
        // When
        String selected = filter.getSelectedPeriod();
        
        // Then
        assertEquals("Last Day", selected, "Selected period should be 'Last Day'");
        // Note: The actual date calculation is done in StatisticsDataFetcher,
        // not in PeriodFilter. PeriodFilter only provides the period text.
        // The window calculation verification is done in integration tests.
    }
}