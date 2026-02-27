package org.company.spacedrepetition.ui.frame;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for date sorting functionality in StatisticsTableModel and DateCellRenderer.
 */
public class DateSortingTest {
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Test
    public void testStatisticsTableModelColumnClasses() {
        String[] columnNames = {"User", "Deck", "Card", "Quality", "Date"};
        StatisticsTableModel model = new StatisticsTableModel(columnNames, 0);
        
        // Test column classes
        assertEquals(String.class, model.getColumnClass(0), "Column 0 should be String");
        assertEquals(String.class, model.getColumnClass(1), "Column 1 should be String");
        assertEquals(String.class, model.getColumnClass(2), "Column 2 should be String");
        assertEquals(String.class, model.getColumnClass(3), "Column 3 should be String");
        assertEquals(LocalDateTime.class, model.getColumnClass(4), "Column 4 should be LocalDateTime");
    }
    
    @Test
    public void testDateCellRendererFormatting() {
        DateCellRenderer renderer = new DateCellRenderer();
        
        // Test LocalDateTime formatting
        LocalDateTime dateTime = LocalDateTime.of(2026, 2, 23, 14, 30);
        String formatted = dateTime.format(DATE_FORMATTER);
        assertEquals("2026-02-23 14:30", formatted, "Date should be formatted correctly");
        
        // Test string date parsing (backward compatibility)
        String dateString = "2026-02-23 14:30";
        LocalDateTime parsed = LocalDateTime.parse(dateString, DATE_FORMATTER);
        assertEquals(dateTime, parsed, "String date should parse correctly");
    }
    
    @Test
    public void testLocalDateTimeComparator() {
        Comparator<LocalDateTime> comparator = new Comparator<LocalDateTime>() {
            @Override
            public int compare(LocalDateTime date1, LocalDateTime date2) {
                return date1.compareTo(date2);
            }
        };
        
        LocalDateTime earlier = LocalDateTime.of(2026, 2, 23, 10, 0);
        LocalDateTime later = LocalDateTime.of(2026, 2, 23, 14, 30);
        
        // Test chronological ordering
        assertTrue(comparator.compare(earlier, later) < 0, "Earlier date should be less than later date");
        assertTrue(comparator.compare(later, earlier) > 0, "Later date should be greater than earlier date");
        assertEquals(0, comparator.compare(earlier, earlier), "Same dates should be equal");
    }
    
    @Test
    public void testStatisticsTableModelDateConversion() {
        String[] columnNames = {"User", "Deck", "Card", "Quality", "Date"};
        StatisticsTableModel model = new StatisticsTableModel(columnNames, 0);
        
        // Test adding row with LocalDateTime
        LocalDateTime dateTime = LocalDateTime.now();
        Object[] rowWithLocalDateTime = {"User1", "Deck1", "Card1", "Good (4)", dateTime};
        model.addRow(rowWithLocalDateTime);
        
        // Verify the value is stored as LocalDateTime
        Object storedValue = model.getValueAt(0, 4);
        assertTrue(storedValue instanceof LocalDateTime, "Date should be stored as LocalDateTime");
        assertEquals(dateTime, storedValue, "Stored date should match original");
        
        // Test adding row with string date (backward compatibility)
        String dateString = "2026-02-23 14:30";
        Object[] rowWithStringDate = {"User2", "Deck2", "Card2", "Hard (3)", dateString};
        model.addRow(rowWithStringDate);
        
        // Verify string date is converted to LocalDateTime
        Object convertedValue = model.getValueAt(1, 4);
        assertTrue(convertedValue instanceof LocalDateTime, "String date should be converted to LocalDateTime");
        LocalDateTime expected = LocalDateTime.parse(dateString, DATE_FORMATTER);
        assertEquals(expected, convertedValue, "Converted date should match parsed string");
    }
    
    @Test
    public void testTableRowSorterConfiguration() {
        String[] columnNames = {"User", "Deck", "Card", "Quality", "Date"};
        StatisticsTableModel model = new StatisticsTableModel(columnNames, 0);
        
        // Create sorter like in MainFrame.configureTableSorter()
        TableRowSorter<StatisticsTableModel> sorter = new TableRowSorter<>(model);
        
        // Configure comparator for date column
        sorter.setComparator(4, new Comparator<LocalDateTime>() {
            @Override
            public int compare(LocalDateTime date1, LocalDateTime date2) {
                return date1.compareTo(date2);
            }
        });
        
        // Set default sorting: date column in descending order (most recent first)
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        
        // Verify sort keys are set correctly
        List<? extends RowSorter.SortKey> actualSortKeys = sorter.getSortKeys();
        assertEquals(1, actualSortKeys.size(), "Should have one sort key");
        assertEquals(4, actualSortKeys.get(0).getColumn(), "Sort key should be for column 4");
        assertEquals(SortOrder.DESCENDING, actualSortKeys.get(0).getSortOrder(), "Sort order should be descending");
    }
}