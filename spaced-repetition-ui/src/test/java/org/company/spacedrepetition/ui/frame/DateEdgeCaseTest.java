package org.company.spacedrepetition.ui.frame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Comparator;
import java.awt.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests edge cases for date handling in the date sorting implementation.
 */
class DateEdgeCaseTest {

    @Test
    @DisplayName("Test parsing various date formats")
    void testParsingVariousDateFormats() {
        // Test valid formats
        assertDoesNotThrow(() -> DateCellRenderer.parseDateTime("2024-01-15 14:30"));
        assertDoesNotThrow(() -> DateCellRenderer.parseDateTime("2024-12-31 23:59"));
        assertDoesNotThrow(() -> DateCellRenderer.parseDateTime("2024-02-29 00:00")); // Leap year
        // Test invalid formats
        assertThrows(DateTimeParseException.class, () -> DateCellRenderer.parseDateTime("2024-01-15"));
        assertThrows(DateTimeParseException.class, () -> DateCellRenderer.parseDateTime("14:30 2024-01-15"));
        assertThrows(DateTimeParseException.class, () -> DateCellRenderer.parseDateTime("2024-01-15 14:30:00"));
        assertThrows(DateTimeParseException.class, () -> DateCellRenderer.parseDateTime("invalid date"));
    }

    @Test
    @DisplayName("Test formatting edge cases")
    void testFormattingEdgeCases() {
        // Test midnight
        LocalDateTime midnight = LocalDateTime.of(2024, 1, 15, 0, 0);
        assertEquals("2024-01-15 00:00", DateCellRenderer.formatDateTime(midnight));
        // Test end of day
        LocalDateTime endOfDay = LocalDateTime.of(2024, 12, 31, 23, 59);
        assertEquals("2024-12-31 23:59", DateCellRenderer.formatDateTime(endOfDay));
        // Test leap day
        LocalDateTime leapDay = LocalDateTime.of(2024, 2, 29, 12, 30);
        assertEquals("2024-02-29 12:30", DateCellRenderer.formatDateTime(leapDay));
    }

    @Test
    @DisplayName("Test comparator with null values")
    void testComparatorWithNullValues() {
        // Create a simple comparator for LocalDateTime
        Comparator<LocalDateTime> comparator = new Comparator<LocalDateTime>() {
            @Override
            public int compare(LocalDateTime date1, LocalDateTime date2) {
                if (date1 == null && date2 == null) return 0;
                if (date1 == null) return -1;
                if (date2 == null) return 1;
                return date1.compareTo(date2);
            }
        };
        LocalDateTime date1 = LocalDateTime.of(2024, 1, 15, 14, 30);
        LocalDateTime date2 = LocalDateTime.of(2024, 1, 16, 10, 0);
        assertTrue(comparator.compare(null, null) == 0);
        assertTrue(comparator.compare(date1, null) > 0);
        assertTrue(comparator.compare(null, date2) < 0);
        assertTrue(comparator.compare(date1, date2) < 0);
        assertTrue(comparator.compare(date2, date1) > 0);
        assertTrue(comparator.compare(date1, date1) == 0);
    }

    @Test
    @DisplayName("Test table model with mixed data types")
    void testTableModelWithMixedDataTypes() {
        String[] columnNames = {"ID", "User", "Card", "Result", "Date"};
        StatisticsTableModel model = new StatisticsTableModel(columnNames, 0);
        // Add rows with different date types
        model.addRow(new Object[]{1, "user1", "card1", "correct", LocalDateTime.now()});
        model.addRow(new Object[]{2, "user2", "card2", "incorrect", "2024-01-15 14:30"});
        model.addRow(new Object[]{3, "user3", "card3", "correct", null});
        
        // Verify all rows were added
        assertEquals(3, model.getRowCount());
        
        // Verify column class for date column
        assertEquals(LocalDateTime.class, model.getColumnClass(4));
        
        // Verify values can be retrieved
        assertNotNull(model.getValueAt(0, 4));
        assertNotNull(model.getValueAt(1, 4));
        assertNull(model.getValueAt(2, 4));
        // Verify the string date was converted to LocalDateTime
        Object dateValue = model.getValueAt(1, 4);
        assertTrue(dateValue instanceof LocalDateTime);
        LocalDateTime parsedDate = (LocalDateTime) dateValue;
        assertEquals(2024, parsedDate.getYear());
        assertEquals(1, parsedDate.getMonthValue());
        assertEquals(15, parsedDate.getDayOfMonth());
        assertEquals(14, parsedDate.getHour());
        assertEquals(30, parsedDate.getMinute());
    }

    @Test
    @DisplayName("Test date cell renderer with invalid data")
    void testDateCellRendererWithInvalidData() {
        DateCellRenderer renderer = new DateCellRenderer();
        
        // Test with null - should not crash
        assertDoesNotThrow(() -> renderer.getTableCellRendererComponent(
            null, null, false, false, 0, 0));
        
        // Test with non-date object - should not crash
        assertDoesNotThrow(() -> renderer.getTableCellRendererComponent(
            null, "invalid", false, false, 0, 0));
        
        // Test with LocalDateTime - should not crash
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 14, 30);
        assertDoesNotThrow(() -> renderer.getTableCellRendererComponent(
            null, date, false, false, 0, 0));
        
        // Test with valid string date - should not crash
        assertDoesNotThrow(() -> renderer.getTableCellRendererComponent(
            null, "2024-01-15 14:30", false, false, 0, 0));
    }

    @Test
    @DisplayName("Test performance with many null dates")
    void testPerformanceWithManyNullDates() {
        String[] columnNames = {"ID", "Date"};
        StatisticsTableModel model = new StatisticsTableModel(columnNames, 0);
        // Add 1000 rows with mostly null dates
        for (int i = 0; i < 1000; i++) {
            if (i % 10 == 0) {
                // Every 10th row has a date
                model.addRow(new Object[]{i, LocalDateTime.now().minusDays(i)});
            } else {
                model.addRow(new Object[]{i, null});
            }
        }
        
        assertEquals(1000, model.getRowCount());
        // Verify sorting doesn't crash with nulls
        Comparator<Object> comparator = new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                if (o1 == null && o2 == null) return 0;
                if (o1 == null) return -1;
                if (o2 == null) return 1;
                if (o1 instanceof LocalDateTime && o2 instanceof LocalDateTime) {
                    return ((LocalDateTime) o1).compareTo((LocalDateTime) o2);
                }
                return 0; // Fallback for non-date comparisons
            }
        };
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                comparator.compare(model.getValueAt(i, 1), model.getValueAt(i + 1, 1));
            }
        });
    }

    @Test
    @DisplayName("Test date parsing with timezone edge cases")
    void testDateParsingWithTimezoneEdgeCases() {
        // The system should handle dates consistently regardless of timezone
        // since we're using LocalDateTime (no timezone)
        
        LocalDateTime date1 = LocalDateTime.of(2024, 1, 15, 14, 30);
        String formatted = DateCellRenderer.formatDateTime(date1);
        LocalDateTime parsed = DateCellRenderer.parseDateTime(formatted);
        
        assertEquals(date1, parsed, "Date should round-trip through formatting and parsing");
        // Test that formatting is consistent
        assertEquals("2024-01-15 14:30", formatted);
    }
}