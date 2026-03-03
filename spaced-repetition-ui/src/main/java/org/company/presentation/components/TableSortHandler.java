package org.company.presentation.components;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик кликов по заголовку таблицы, реализующий специальную логику сортировки:
 * - для столбца по умолчанию (например, Date): ASC → DESC → без сортировки
 * - для остальных столбцов: ASC → DESC → сброс к сортировке по умолчанию
 */
public class TableSortHandler {
    private final TableRowSorter<?> sorter;
    private final int defaultSortColumn;
    private final SortOrder defaultSortOrder;

    /**
     * Создаёт обработчик с заданными параметрами сортировки по умолчанию.
     * @param sorter сортировщик таблицы
     * @param defaultSortColumn индекс столбца для сортировки по умолчанию
     * @param defaultSortOrder порядок сортировки по умолчанию
     */
    public TableSortHandler(TableRowSorter<?> sorter, int defaultSortColumn, SortOrder defaultSortOrder) {
        this.sorter = sorter;
        this.defaultSortColumn = defaultSortColumn;
        this.defaultSortOrder = defaultSortOrder;
    }

    /**
     * Обрабатывает клик по заголовку столбца.
     * @param column индекс столбца, по которому кликнули
     */
    public void handleColumnClick(int column) {
        List<? extends RowSorter.SortKey> currentKeys = sorter.getSortKeys();
        SortOrder currentOrder = null;
        if (currentKeys != null && !currentKeys.isEmpty() && currentKeys.get(0).getColumn() == column) {
            currentOrder = currentKeys.get(0).getSortOrder();
        }

        List<RowSorter.SortKey> newKeys = new ArrayList<>();

        if (column == defaultSortColumn) {
            // Для столбца по умолчанию: ASC → DESC → без сортировки
            if (currentOrder == null) {
                newKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
            } else if (currentOrder == SortOrder.ASCENDING) {
                newKeys.add(new RowSorter.SortKey(column, SortOrder.DESCENDING));
            } // иначе (было DESCENDING) → без сортировки (newKeys пуст)
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
     * Сбрасывает сортировку к состоянию по умолчанию.
     */
    public void resetToDefaultSort() {
        List<RowSorter.SortKey> defaultKeys = new ArrayList<>();
        defaultKeys.add(new RowSorter.SortKey(defaultSortColumn, defaultSortOrder));
        sorter.setSortKeys(defaultKeys);
    }
}