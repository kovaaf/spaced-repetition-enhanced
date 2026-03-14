package org.company.presentation.components;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles cyclic sorting behaviour for a {@link TableRowSorter}.
 * Provides a custom click cycle: for the default column it toggles between ASC/DESC,
 * for other columns it cycles ASC → DESC → default sort.
 */
public class TableSortHandler {
    private final TableRowSorter<?> sorter;
    private final int defaultSortColumn;
    private final SortOrder defaultSortOrder;

    public TableSortHandler(TableRowSorter<?> sorter, int defaultSortColumn, SortOrder defaultSortOrder) {
        this.sorter = sorter;
        this.defaultSortColumn = defaultSortColumn;
        this.defaultSortOrder = defaultSortOrder;
    }

    /**
     * Processes a click on a column header.
     *
     * @param column the index of the clicked column
     */
    public void handleColumnClick(int column) {
        List<? extends RowSorter.SortKey> currentKeys = sorter.getSortKeys();
        SortOrder currentOrder = null;
        if (currentKeys != null && !currentKeys.isEmpty() && currentKeys.get(0).getColumn() == column) {
            currentOrder = currentKeys.get(0).getSortOrder();
        }

        List<RowSorter.SortKey> newKeys = new ArrayList<>();

        if (column == defaultSortColumn) {
            // Для столбца по умолчанию (Date): ASC → DESC → ASC (циклически)
            if (currentOrder == null || currentOrder == SortOrder.DESCENDING) {
                newKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
            } else if (currentOrder == SortOrder.ASCENDING) {
                newKeys.add(new RowSorter.SortKey(column, SortOrder.DESCENDING));
            }
        } else {
            // Для остальных столбцов: ASC → DESC → сортировка по умолчанию
            if (currentOrder == null) {
                newKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
            } else if (currentOrder == SortOrder.ASCENDING) {
                newKeys.add(new RowSorter.SortKey(column, SortOrder.DESCENDING));
            } else { // было DESCENDING → возвращаем сортировку по умолчанию
                newKeys.add(new RowSorter.SortKey(defaultSortColumn, defaultSortOrder));
            }
        }

        sorter.setSortKeys(newKeys.isEmpty() ? null : newKeys);
    }

    /**
     * Resets the sort order to the default (sort by default column in default order).
     */
    public void resetToDefaultSort() {
        List<RowSorter.SortKey> defaultKeys = new ArrayList<>();
        defaultKeys.add(new RowSorter.SortKey(defaultSortColumn, defaultSortOrder));
        sorter.setSortKeys(defaultKeys);
    }
}