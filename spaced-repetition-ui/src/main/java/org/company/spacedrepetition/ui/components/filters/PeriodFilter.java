package org.company.spacedrepetition.ui.components.filters;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Period filter component for selecting time ranges.
 * Provides five mutually exclusive toggle buttons:
 * - Last Day
 * - Last Week
 * - Last Month  
 * - Last Year
 * - All Time
 * 
 * Follows TRD Appendix E specifications for filter components.
 * Uses a ButtonGroup to ensure only one period is selected at a time.
 */
public class PeriodFilter extends JPanel {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodFilter.class);
    
    private JToggleButton lastWeekButton;
    private JToggleButton lastMonthButton;
    private JToggleButton lastYearButton;
    private JToggleButton allTimeButton;
    private JToggleButton lastDayButton;
    private ButtonGroup buttonGroup;
    
    /**
     * Constructs a new PeriodFilter with default styling and layout.
     * "Last Month" is selected by default as the most common use case.
     */
    public PeriodFilter() {
        initializeComponents();
        setupLayout();
        setupStyling();
        setupListeners();
        
        // Set default selection
        lastMonthButton.setSelected(true);
    }
    
    /**
     * Initializes the toggle buttons and button group.
     */
    private void initializeComponents() {
        lastWeekButton = new JToggleButton("Last Week");
        lastMonthButton = new JToggleButton("Last Month");
        lastYearButton = new JToggleButton("Last Year");
        allTimeButton = new JToggleButton("All Time");
        lastDayButton = new JToggleButton("Last Day");
        buttonGroup = new ButtonGroup();
        buttonGroup.add(lastWeekButton);
        buttonGroup.add(lastMonthButton);
        buttonGroup.add(lastYearButton);
        buttonGroup.add(allTimeButton);
        buttonGroup.add(lastDayButton);
    }
    
    /**
     * Sets up the layout for the filter buttons.
     * Uses GridLayout with 1 row and 5 columns for horizontal arrangement.
     */
    private void setupLayout() {
        setLayout(new GridLayout(1, 5, 5, 0)); // 1 row, 5 columns, 5px horizontal gap
    }
    
    /**
     * Applies consistent styling to all components.
     * Follows the existing UI's Dialog font and color scheme.
     */
    private void setupStyling() {
        Font buttonFont = new Font("Dialog", Font.PLAIN, 12);
        lastWeekButton.setFont(buttonFont);
        lastMonthButton.setFont(buttonFont);
        lastYearButton.setFont(buttonFont);
        allTimeButton.setFont(buttonFont);
        lastDayButton.setFont(buttonFont);
        // Add visual border similar to placeholder components
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        // Add buttons to panel
        add(lastDayButton);
        add(lastWeekButton);
        add(lastMonthButton);
        add(lastYearButton);
        add(allTimeButton);
    }
    
    /**
     * Sets up action listeners for the toggle buttons.
     * Each button prints its selection to console for verification.
     */
    private void setupListeners() {
        ActionListener periodListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JToggleButton source = (JToggleButton) e.getSource();
                if (source.isSelected()) {
                    LOG.info("Period selected: " + source.getText());
                }
            }
        };
        
        lastWeekButton.addActionListener(periodListener);
        lastMonthButton.addActionListener(periodListener);
        lastYearButton.addActionListener(periodListener);
        allTimeButton.addActionListener(periodListener);
        lastDayButton.addActionListener(periodListener);
    }
    
    /**
     * Gets the currently selected period.
     * @return the text of the selected button, or empty string if none selected
     */
    public String getSelectedPeriod() {
        if (lastWeekButton.isSelected()) {
            return lastWeekButton.getText();
        } else if (lastMonthButton.isSelected()) {
            return lastMonthButton.getText();
        } else if (lastYearButton.isSelected()) {
            return lastYearButton.getText();
        } else if (allTimeButton.isSelected()) {
            return allTimeButton.getText();
        } else if (lastDayButton.isSelected()) {
            return lastDayButton.getText();
        }
        return "";
    }
    
    /**
     * Sets the selected period by button text.
     *
     * @param periodText the text of the period button to select
     */
    public void setSelectedPeriod(String periodText) {
        switch (periodText) {
            case "Last Week" -> lastWeekButton.setSelected(true);
            case "Last Month" -> lastMonthButton.setSelected(true);
            case "Last Year" -> lastYearButton.setSelected(true);
            case "All Time" -> allTimeButton.setSelected(true);
            case "Last Day" -> lastDayButton.setSelected(true);
            default -> { }
        }
    }
}