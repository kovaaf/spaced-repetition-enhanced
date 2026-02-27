package org.company.spacedrepetition.ui.frame;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance test for date sorting with up to 1000 rows.
 */
public class DateSortingPerformanceTest {
    
    private static final int MAX_ROWS = 1000;
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility
    
    @Test
    public void testPerformanceWith1000Rows() {
        String[] columnNames = {"User", "Deck", "Card", "Quality", "Date"};
        StatisticsTableModel model = new StatisticsTableModel(columnNames, 0);
        
        // Generate 1000 rows with random dates
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < MAX_ROWS; i++) {
            LocalDateTime dateTime = generateRandomDateTime();
            Object[] row = {
                "User" + (i % 10),
                "Deck" + (i % 5),
                "Card" + i,
                "Good (4)",
                dateTime
            };
            model.addRow(row);
        }
        
        long addTime = System.currentTimeMillis() - startTime;
        System.out.println("Added " + MAX_ROWS + " rows in " + addTime + "ms");
        
        // Verify we have the correct number of rows
        assertEquals(MAX_ROWS, model.getRowCount(), "Should have " + MAX_ROWS + " rows");
        
        // Create and configure sorter
        TableRowSorter<StatisticsTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(4, new Comparator<LocalDateTime>() {
            @Override
            public int compare(LocalDateTime date1, LocalDateTime date2) {
                return date1.compareTo(date2);
            }
        });
        
        // Test sorting performance
        startTime = System.currentTimeMillis();
        
        // Sort by date in descending order (most recent first)
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        
        long sortTime = System.currentTimeMillis() - startTime;
        System.out.println("Sorted " + MAX_ROWS + " rows in " + sortTime + "ms");
        
        // Performance assertions
        assertTrue(addTime < 1000, "Adding " + MAX_ROWS + " rows should take less than 1000ms");
        assertTrue(sortTime < 500, "Sorting " + MAX_ROWS + " rows should take less than 500ms");
        
        // Verify sorting works correctly
        verifySortingOrder(sorter, model, SortOrder.DESCENDING);
        
        // Test ascending sort
        startTime = System.currentTimeMillis();
        sortKeys.clear();
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        
        sortTime = System.currentTimeMillis() - startTime;
        System.out.println("Re-sorted " + MAX_ROWS + " rows in " + sortTime + "ms");
        
        verifySortingOrder(sorter, model, SortOrder.ASCENDING);
    }
    
    @Test
    public void testDateCellRendererPerformance() {
        DateCellRenderer renderer = new DateCellRenderer();
        
        // Generate 1000 dates
        LocalDateTime[] dates = new LocalDateTime[MAX_ROWS];
        for (int i = 0; i < MAX_ROWS; i++) {
            dates[i] = generateRandomDateTime();
        }
        
        // Test rendering performance
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < MAX_ROWS; i++) {
            // Simulate rendering call
            renderer.getTableCellRendererComponent(null, dates[i], false, false, i, 4);
        }
        
        long renderTime = System.currentTimeMillis() - startTime;
        System.out.println("Rendered " + MAX_ROWS + " dates in " + renderTime + "ms");
        
        assertTrue(renderTime < 500, "Rendering " + MAX_ROWS + " dates should take less than 500ms");
    }
    
    @Test
    public void testBackwardCompatibilityPerformance() {
        String[] columnNames = {"User", "Deck", "Card", "Quality", "Date"};
        StatisticsTableModel model = new StatisticsTableModel(columnNames, 0);
        
        // Mix of LocalDateTime and string dates
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < MAX_ROWS; i++) {
            Object dateValue;
            if (i % 2 == 0) {
                // LocalDateTime
                dateValue = generateRandomDateTime();
            } else {
                // String date (backward compatibility)
                LocalDateTime dateTime = generateRandomDateTime();
                dateValue = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            
            Object[] row = {
                "User" + (i % 10),
                "Deck" + (i % 5),
                "Card" + i,
                "Good (4)",
                dateValue
            };
            model.addRow(row);
        }
        
        long addTime = System.currentTimeMillis() - startTime;
        System.out.println("Added " + MAX_ROWS + " mixed-format rows in " + addTime + "ms");
        
        // Verify all dates are converted to LocalDateTime
        for (int i = 0; i < model.getRowCount(); i++) {
            Object value = model.getValueAt(i, 4);
            assertTrue(value instanceof LocalDateTime, 
                "Row " + i + " should have LocalDateTime, got: " + value.getClass().getName());
        }
        
        assertTrue(addTime < 1500, "Adding " + MAX_ROWS + " mixed-format rows should take less than 1500ms");
    }
    
    private LocalDateTime generateRandomDateTime() {
        // Generate random date within the last year
        LocalDateTime now = LocalDateTime.now();
        long daysAgo = RANDOM.nextInt(365);
        long hoursAgo = RANDOM.nextInt(24);
        long minutesAgo = RANDOM.nextInt(60);
        
        return now.minusDays(daysAgo)
                  .minusHours(hoursAgo)
                  .minusMinutes(minutesAgo)
                  .truncatedTo(ChronoUnit.MINUTES);
    }
    
    private void verifySortingOrder(TableRowSorter<StatisticsTableModel> sorter, 
                                   StatisticsTableModel model, 
                                   SortOrder sortOrder) {
        
        // Get the sorted view
        int rowCount = model.getRowCount();
        
        // Check that dates are in correct order
        for (int i = 0; i < rowCount - 1; i++) {
            int modelIndex1 = sorter.convertRowIndexToModel(i);
            int modelIndex2 = sorter.convertRowIndexToModel(i + 1);
            
            LocalDateTime date1 = (LocalDateTime) model.getValueAt(modelIndex1, 4);
            LocalDateTime date2 = (LocalDateTime) model.getValueAt(modelIndex2, 4);
            
            if (sortOrder == SortOrder.DESCENDING) {
                // Most recent first
                assertTrue(date1.compareTo(date2) >= 0, 
                    "Row " + i + " should be >= row " + (i + 1) + " in descending order");
            } else {
                // Oldest first
                assertTrue(date1.compareTo(date2) <= 0, 
                    "Row " + i + " should be <= row " + (i + 1) + " in ascending order");
            }
        }
    }
}