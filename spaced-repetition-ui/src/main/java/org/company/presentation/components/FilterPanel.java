package org.company.presentation.components;

import org.company.domain.TimeFilter;
import org.company.presentation.presenter.FilterPresenter;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing buttons for each predefined time filter.
 * Clicking a button delegates the action to the {@link FilterPresenter}.
 */
public class FilterPanel extends JPanel {
    public FilterPanel(FilterPresenter presenter) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        add(createFilterButton("Last Day", TimeFilter.LAST_DAY, presenter));
        add(createFilterButton("Last Week", TimeFilter.LAST_WEEK, presenter));
        add(createFilterButton("Last Month", TimeFilter.LAST_MONTH, presenter));
        add(createFilterButton("Last Year", TimeFilter.LAST_YEAR, presenter));
        add(createFilterButton("All Time", TimeFilter.ALL_TIME, presenter));
    }

    private JButton createFilterButton(String label, TimeFilter filter, FilterPresenter presenter) {
        JButton button = new JButton(label);
        button.addActionListener(e -> presenter.onFilterSelected(filter));
        return button;
    }
}