package org.company.spacedrepetition.ui.frame;

import javax.swing.table.DefaultTableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Custom table model for statistics table that properly handles date sorting.
 * Stores LocalDateTime objects in the date column for natural chronological sorting.
 * Provides backward compatibility with string dates during transition.
 */
public class StatisticsTableModel extends DefaultTableModel {
    private static final int DATE_COLUMN_INDEX = 4;
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    /**
     * Creates a new StatisticsTableModel with the specified column names and row count.
     * 
     * @param columnNames the column names
     * @param rowCount the initial row count
     */
    public StatisticsTableModel(String[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case DATE_COLUMN_INDEX:
                return LocalDateTime.class;
            default:
                return String.class;
        }
    }
    
    @Override
    public void addRow(Object[] rowData) {
        // Ensure date column contains LocalDateTime
        convertDateStringToLocalDateTime(rowData);
        super.addRow(rowData);
    }
    
    @Override
    public void insertRow(int row, Object[] rowData) {
        // Ensure date column contains LocalDateTime
        convertDateStringToLocalDateTime(rowData);
        super.insertRow(row, rowData);
    }
    
    @Override
    public void setValueAt(Object value, int row, int column) {
        // Convert string to LocalDateTime if setting date column
        if (column == DATE_COLUMN_INDEX && value instanceof String) {
            try {
                value = LocalDateTime.parse((String) value, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                // Keep as string if parsing fails
            }
        }
        super.setValueAt(value, row, column);
    }
    
    /**
     * Converts date string to LocalDateTime in the row data if needed.
     * Provides backward compatibility during transition.
     * 
     * @param rowData the row data array
     */
    private void convertDateStringToLocalDateTime(Object[] rowData) {
        if (rowData.length > DATE_COLUMN_INDEX) {
            Object dateValue = rowData[DATE_COLUMN_INDEX];
            if (dateValue instanceof String) {
                try {
                    rowData[DATE_COLUMN_INDEX] = LocalDateTime.parse(
                        (String) dateValue, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    // Keep as string if parsing fails
                    // This maintains backward compatibility
                }
            }
        }
    }
    
    /**
     * Gets all LocalDateTime values from the date column.
     * Useful for debugging and testing.
     * 
     * @return list of LocalDateTime objects
     */
    public List<LocalDateTime> getDateValues() {
        List<LocalDateTime> dates = new ArrayList<>();
        for (int i = 0; i < getRowCount(); i++) {
            Object value = getValueAt(i, DATE_COLUMN_INDEX);
            if (value instanceof LocalDateTime) {
                dates.add((LocalDateTime) value);
            }
        }
        return dates;
    }
    
    /**
     * Checks if the date column contains LocalDateTime objects.
     * 
     * @return true if all date values are LocalDateTime objects
     */
    public boolean isDateColumnUsingLocalDateTime() {
        for (int i = 0; i < getRowCount(); i++) {
            Object value = getValueAt(i, DATE_COLUMN_INDEX);
            if (!(value instanceof LocalDateTime)) {
                return false;
            }
        }
        return true;
    }
}