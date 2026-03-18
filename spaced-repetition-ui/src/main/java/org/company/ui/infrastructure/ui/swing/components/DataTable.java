package org.company.ui.infrastructure.ui.swing.components;

import org.company.ui.domain.entity.AnswerEvent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom JTable that displays answer events.
 * Provides column rendering, sorting (via header click) and a specialized quality renderer.
 */
public class DataTable extends JTable {
    private final AnswerTableModel model;
    private final TableSortHandler sortHandler;

    public DataTable() {
        this.model = new AnswerTableModel();
        setModel(model);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<AnswerTableModel> sorter = new TableRowSorter<>(model);
        setRowSorter(sorter);

        for (int i = 0; i < model.getColumnCount(); i++) {
            sorter.setSortable(i, false);
        }

        this.sortHandler = new TableSortHandler(sorter, 4, SortOrder.DESCENDING);

        JTableHeader header = getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = header.columnAtPoint(e.getPoint());
                if (col >= 0) {
                    sortHandler.handleColumnClick(col);
                }
            }
        });

        TableColumn qualityColumn = getColumnModel().getColumn(3);
        qualityColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Integer) {
                    setText(qualityToString((Integer) value));
                } else {
                    super.setValue(value);
                }
            }
        });
    }

    private String qualityToString(int quality) {
        return switch (quality) {
            case 0 -> "Again";
            case 3 -> "Hard";
            case 4 -> "Good";
            case 5 -> "Easy";
            default -> String.valueOf(quality);
        };
    }

    /**
     * Replaces the entire table content with the given list.
     *
     * @param data new list of events
     */
    public void setData(List<AnswerEvent> data) {
        model.setData(data);
        sortHandler.resetToDefaultSort();
    }

    /**
     * Appends a single event to the table.
     *
     * @param event the event to add
     */
    public void addEvent(AnswerEvent event) {
        model.addEvent(event);
    }

    /**
     * Removes all rows from the table.
     */
    public void clearData() {
        model.clearData();
    }

    private static class AnswerTableModel extends AbstractTableModel {
        private final String[] columns = {"User", "Deck", "Card", "Quality", "Date"};
        private final List<AnswerEvent> data = new ArrayList<>();
        private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        public void setData(List<AnswerEvent> newData) {
            data.clear();
            data.addAll(newData);
            fireTableDataChanged();
        }

        public void addEvent(AnswerEvent event) {
            data.add(event);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public void clearData() {
            data.clear();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AnswerEvent e = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> e.userName() != null ? e.userName() : e.userId();
                case 1 -> e.deckName() != null ? e.deckName() : e.deckId();
                case 2 -> e.cardTitle() != null ? e.cardTitle() : e.cardId();
                case 3 -> e.quality();
                case 4 -> timeFormatter.format(e.timestamp());
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 1, 2, 4 -> String.class;
                case 3 -> Integer.class;
                default -> Object.class;
            };
        }
    }
}