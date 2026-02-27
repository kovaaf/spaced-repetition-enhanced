package org.company.spacedrepetition.ui.frame;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import javax.swing.JTable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Cell renderer for date columns that formats LocalDateTime objects
 * as "yyyy-MM-dd HH:mm" strings for display.
 * Provides backward compatibility with string dates during transition.
 */
public class DateCellRenderer extends DefaultTableCellRenderer {
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        Object displayValue = value;
        
        // Format LocalDateTime objects for display
        if (value instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) value;
            displayValue = dateTime.format(DATE_FORMATTER);
        } else if (value instanceof String) {
            // Handle string dates (backward compatibility)
            // Try to parse and reformat for consistency
            try {
                LocalDateTime dateTime = LocalDateTime.parse((String) value, DATE_FORMATTER);
                displayValue = dateTime.format(DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                // Keep original string if parsing fails
                displayValue = value;
            }
        }
        
        Component c = super.getTableCellRendererComponent(table, displayValue, isSelected, 
            hasFocus, row, column);
        
        // Apply consistent styling
        setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        
        return c;
    }
    
    /**
     * Formats a LocalDateTime object as a display string.
     * 
     * @param dateTime the LocalDateTime to format
     * @return formatted string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }
    
    /**
     * Parses a string to LocalDateTime using the standard format.
     * 
     * @param dateString the date string to parse
     * @return LocalDateTime object
     * @throws DateTimeParseException if parsing fails
     */
    public static LocalDateTime parseDateTime(String dateString) throws DateTimeParseException {
        return LocalDateTime.parse(dateString, DATE_FORMATTER);
    }
}