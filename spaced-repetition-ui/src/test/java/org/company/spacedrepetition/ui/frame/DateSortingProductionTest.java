package org.company.spacedrepetition.ui.frame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.table.TableRowSorter;

/**
 * Production performance monitoring test for date sorting.
 * This test simulates real-world scenarios with mixed date formats
 * and verifies performance characteristics.
 */
public class DateSortingProductionTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility
    
    @Test
    @DisplayName("Production: Mixed date formats with realistic distribution")
    void testMixedDateFormatsRealisticDistribution() {
        StatisticsTableModel model = new StatisticsTableModel(new String[]{"User", "Deck", "Card", "Quality", "Date"}, 0);
        TableRowSorter<StatisticsTableModel> sorter = new TableRowSorter<>(model);
        
        // Configure sorter with LocalDateTime comparator
        sorter.setComparator(4, (o1, o2) -> {
            if (o1 instanceof LocalDateTime && o2 instanceof LocalDateTime) {
                return ((LocalDateTime) o1).compareTo((LocalDateTime) o2);
            }
            return 0;
        });
        
        // Generate realistic data distribution
        List<Object[]> testData = generateRealisticTestData(1000);
        
        long startTime = System.currentTimeMillis();
        
        // Add data to model
        for (Object[] row : testData) {
            model.addRow(row);
        }
        
        long addTime = System.currentTimeMillis() - startTime;
        System.out.println("Production: Added 1000 realistic rows in " + addTime + "ms");
        
        // Test sorting performance
        startTime = System.currentTimeMillis();
        sorter.toggleSortOrder(4); // Sort by date column
        long sortTime = System.currentTimeMillis() - startTime;
        System.out.println("Production: Sorted 1000 realistic rows in " + sortTime + "ms");
        
        // Verify sorting is efficient
        assert sortTime < 100 : "Sorting should be efficient (<100ms)";
        assert addTime < 500 : "Adding rows should be efficient (<500ms)";
    }
    
    @Test
    @DisplayName("Production: Backward compatibility with legacy string dates")
    void testBackwardCompatibilityPerformance() {
        StatisticsTableModel model = new StatisticsTableModel(new String[]{"User", "Deck", "Card", "Quality", "Date"}, 0);
        
        // Generate mixed data: 50% LocalDateTime, 50% legacy string dates
        List<Object[]> testData = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            LocalDateTime date = LocalDateTime.now().minusDays(RANDOM.nextInt(365));
            testData.add(new Object[]{
                "User" + i,
                "Deck" + (i % 10),
                "Card" + i,
                "GOOD",
                date
            });
        }
        
        for (int i = 500; i < 1000; i++) {
            LocalDateTime date = LocalDateTime.now().minusDays(RANDOM.nextInt(365));
            testData.add(new Object[]{
                "User" + i,
                "Deck" + (i % 10),
                "Card" + i,
                "GOOD",
                date.format(FORMATTER) // Legacy string format
            });
        }
        
        long startTime = System.currentTimeMillis();
        
        // Add mixed data
        for (Object[] row : testData) {
            model.addRow(row);
        }
        
        long addTime = System.currentTimeMillis() - startTime;
        System.out.println("Production: Added 1000 mixed-format rows in " + addTime + "ms");
        
        // Verify all dates are properly stored as LocalDateTime
        for (int i = 0; i < model.getRowCount(); i++) {
            Object dateValue = model.getValueAt(i, 4);
            assert dateValue instanceof LocalDateTime : 
                "All dates should be converted to LocalDateTime, found: " + 
                (dateValue != null ? dateValue.getClass().getName() : "null");
        }
        
        assert addTime < 1000 : "Mixed format handling should be efficient (<1000ms)";
    }
    
    @Test
    @DisplayName("Production: Memory usage with large datasets")
    void testMemoryUsageWithLargeDatasets() {
        StatisticsTableModel model = new StatisticsTableModel(new String[]{"User", "Deck", "Card", "Quality", "Date"}, 0);
        
        // Get initial memory usage
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Add large dataset
        List<Object[]> testData = generateRealisticTestData(5000);
        
        long startTime = System.currentTimeMillis();
        for (Object[] row : testData) {
            model.addRow(row);
        }
        long addTime = System.currentTimeMillis() - startTime;
        
        // Get final memory usage
        runtime.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        
        System.out.println("Production: Added 5000 rows in " + addTime + "ms");
        System.out.println("Production: Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
        
        // Performance assertions
        assert addTime < 5000 : "Adding 5000 rows should be efficient (<5000ms)";
        assert memoryUsed < 50 * 1024 * 1024 : "Memory usage should be reasonable (<50MB)";
    }
    
    /**
     * Generate realistic test data with varied date distribution.
     */
    private List<Object[]> generateRealisticTestData(int count) {
        List<Object[]> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Realistic distribution: more recent events are more common
        for (int i = 0; i < count; i++) {
            // Exponential distribution favoring recent dates
            int daysAgo = (int) Math.pow(RANDOM.nextDouble() * 365, 0.7);
            LocalDateTime date = now.minusDays(daysAgo)
                                   .minusHours(RANDOM.nextInt(24))
                                   .minusMinutes(RANDOM.nextInt(60));
            
            data.add(new Object[]{
                "User" + (i % 100),           // 100 unique users
                "Deck" + (i % 20),            // 20 unique decks
                "Card" + i,                   // Unique cards
                getRandomQuality(),           // Random quality
                date                          // LocalDateTime object
            });
        }
        
        return data;
    }
    
    /**
     * Get random quality with realistic distribution.
     * In production, GOOD and EASY are more common than AGAIN and HARD.
     */
    private String getRandomQuality() {
        double rand = RANDOM.nextDouble();
        if (rand < 0.1) return "AGAIN";   // 10%
        if (rand < 0.3) return "HARD";    // 20%
        if (rand < 0.7) return "GOOD";    // 40%
        return "EASY";                    // 30%
    }
    
    @Test
    @DisplayName("Production: Date sorting maintains chronological order")
    void testDateSortingChronologicalOrder() {
        StatisticsTableModel model = new StatisticsTableModel(new String[]{"User", "Deck", "Card", "Quality", "Date"}, 0);
        List<LocalDateTime> dates = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int i = 0; i < 100; i++) {
            LocalDateTime date = baseDate.plusDays(RANDOM.nextInt(365))
                                         .plusHours(RANDOM.nextInt(24))
                                         .plusMinutes(RANDOM.nextInt(60));
            dates.add(date);
            model.addRow(new Object[]{"User", "Deck", "Card", "GOOD", date});
        }
        
        // Create a JTable with the model to properly test sorting
        javax.swing.JTable table = new javax.swing.JTable(model);
        TableRowSorter<StatisticsTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(4, (o1, o2) -> {
            if (o1 instanceof LocalDateTime && o2 instanceof LocalDateTime) {
                return ((LocalDateTime) o1).compareTo((LocalDateTime) o2);
            }
            return 0;
        });
        
        // Set the sorter on the table
        table.setRowSorter(sorter);
        
        // Sort ascending by date column
        table.getRowSorter().setSortKeys(List.of(new TableRowSorter.SortKey(4, javax.swing.SortOrder.ASCENDING)));
        
        // Verify chronological order by checking table values through sorted view
        for (int i = 0; i < table.getRowCount() - 1; i++) {
            int modelIndex1 = table.convertRowIndexToModel(i);
            int modelIndex2 = table.convertRowIndexToModel(i + 1);
            
            LocalDateTime current = (LocalDateTime) model.getValueAt(modelIndex1, 4);
            LocalDateTime next = (LocalDateTime) model.getValueAt(modelIndex2, 4);
            assert current.compareTo(next) <= 0 : 
                "Dates should be in chronological order in sorted view: " + current + " should be <= " + next + " (view indices: " + i + " -> " + (i + 1) + ")";
        }
        System.out.println("Production: Verified chronological ordering for 100 rows");
    }
}