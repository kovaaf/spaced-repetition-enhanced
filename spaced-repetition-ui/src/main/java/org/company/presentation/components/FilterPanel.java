package org.company.presentation.components;

import org.company.application.FilterController;
import org.company.domain.TimeFilter;

import javax.swing.*;
import java.awt.*;

public class FilterPanel extends JPanel {
    public FilterPanel(FilterController controller) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        add(createFilterButton("Last Day", TimeFilter.LAST_DAY, controller));
        add(createFilterButton("Last Week", TimeFilter.LAST_WEEK, controller));
        add(createFilterButton("Last Month", TimeFilter.LAST_MONTH, controller));
        add(createFilterButton("Last Year", TimeFilter.LAST_YEAR, controller));
        add(createFilterButton("All Time", TimeFilter.ALL_TIME, controller));
    }

    private JButton createFilterButton(String label, TimeFilter filter, FilterController controller) {
        JButton button = new JButton(label);
        button.addActionListener(e -> controller.applyFilter(filter));
        return button;
    }
}