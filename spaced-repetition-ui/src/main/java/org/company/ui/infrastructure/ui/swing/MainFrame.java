package org.company.ui.infrastructure.ui.swing;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.ui.application.port.output.ui.TaskView;
import org.company.ui.domain.entity.AnswerEvent;
import org.company.ui.domain.entity.ServerInfo;
import org.company.ui.infrastructure.ui.swing.components.DataTable;
import org.company.ui.infrastructure.ui.swing.components.FilterPanel;
import org.company.ui.infrastructure.ui.swing.components.LogPanel;
import org.company.ui.infrastructure.ui.swing.components.StatusBar;
import org.company.ui.infrastructure.ui.swing.presenter.FilterPresenter;
import org.company.ui.infrastructure.ui.swing.presenter.ServerPresenter;
import org.company.ui.infrastructure.ui.swing.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window.
 * Implements {@link TaskView} and delegates user actions to the presenters.
 */
@Slf4j
public class MainFrame extends JFrame implements TaskView {
    private final DataTable dataTable;
    private final StatusBar statusBar;
    @Getter
    private final LogPanel logPanel;
    private final JSplitPane splitPane;
    private boolean logsVisible = false;
    private int lastDividerLocation = 0;

    private final List<JRadioButtonMenuItem> serverMenuItems = new ArrayList<>();

    @Setter
    private FilterPresenter filterPresenter;
    @Setter
    private ServerPresenter serverPresenter;

    private FilterPanel filterPanel;

    public MainFrame(List<ServerInfo> servers, String defaultServerUrl) {
        this.dataTable = new DataTable();
        this.statusBar = new StatusBar(this);
        this.logPanel = new LogPanel();

        JScrollPane tableScroll = new JScrollPane(dataTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Данные"));
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, null);
        splitPane.setResizeWeight(1.0);

        initUI();
        createMenuBar(servers, defaultServerUrl);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (filterPresenter != null) {
                    filterPresenter.shutdown();
                }
            }
        });
    }

    private void initUI() {
        setTitle("Data Viewer");
        setLayout(new BorderLayout());

        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setSize(1024, 720);
        setLocationRelativeTo(null);
    }

    private void createMenuBar(List<ServerInfo> servers, String defaultServerUrl) {
        JMenuBar menuBar = new JMenuBar();

        JMenu themeMenu = getThemeMenu();
        menuBar.add(themeMenu);

        JMenu serverMenu = new JMenu("Source Server");
        ButtonGroup serverGroup = new ButtonGroup();

        for (ServerInfo server : servers) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(server.name());
            item.addActionListener(e -> {
                if (item.isSelected() && serverPresenter != null) {
                    serverPresenter.onServerSelected(server.name());
                }
            });
            serverGroup.add(item);
            serverMenu.add(item);
            serverMenuItems.add(item);
            if (server.url().equals(defaultServerUrl)) {
                item.setSelected(true);
            }
        }
        menuBar.add(serverMenu);

        setJMenuBar(menuBar);
    }

    private JMenu getThemeMenu() {
        JMenu themeMenu = new JMenu("Theme");
        ButtonGroup themeGroup = new ButtonGroup();
        String[] themeNames = ThemeManager.getAvailableThemeNames();

        for (String name : themeNames) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
            item.addActionListener(e -> {
                if (item.isSelected()) {
                    ThemeManager.applyTheme(name);
                }
            });
            themeGroup.add(item);
            themeMenu.add(item);
            if (name.equals("Dark")) {
                item.setSelected(true);
            }
        }
        return themeMenu;
    }

    /**
     * Ensures that the filter panel is created and added to the frame.
     * Should be called after the filter presenter is set.
     */
    public void ensureFilterPanel() {
        if (filterPanel == null && filterPresenter != null) {
            filterPanel = new FilterPanel(filterPresenter);
            add(filterPanel, BorderLayout.NORTH);
            revalidate();
            repaint();
        }
    }

    /**
     * Toggles the visibility of the log panel.
     */
    public void toggleLogs() {
        if (logsVisible) {
            lastDividerLocation = splitPane.getDividerLocation();
            splitPane.setBottomComponent(null);
            statusBar.setLogButtonText(false);
        } else {
            splitPane.setBottomComponent(logPanel);
            if (lastDividerLocation > 0) {
                splitPane.setDividerLocation(lastDividerLocation);
            } else {
                SwingUtilities.invokeLater(() -> {
                    int desiredLogHeight = 150;
                    int totalHeight = splitPane.getHeight();
                    if (totalHeight > 0) {
                        splitPane.setDividerLocation(totalHeight - desiredLogHeight);
                    } else {
                        splitPane.setDividerLocation(0.8);
                    }
                    JScrollBar vertical = logPanel.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                });
            }
            statusBar.setLogButtonText(true);
        }
        logsVisible = !logsVisible;
    }

    /**
     * Cancels ongoing data loading (delegates to the filter presenter).
     */
    public void cancelLoading() {
        if (filterPresenter != null) {
            filterPresenter.cancelLoading();
        }
    }

    // === TaskView implementation ===
    @Override
    public void setRunningState(boolean running) {
        statusBar.setProgressVisible(running);
        if (running) {
            statusBar.setSuccessIconVisible(false);
        }
    }

    @Override
    public void setProgressIndeterminate(boolean indeterminate) {
        statusBar.setProgressIndeterminate(indeterminate);
    }

    @Override
    public void setStatus(String status) {
        statusBar.setStatus(status);
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
        statusBar.setStatus("Ошибка");
        statusBar.setSuccessIconVisible(false);
    }

    @Override
    public void onTaskCancelled() {
        statusBar.setStatus("Отменено");
        statusBar.setSuccessIconVisible(false);
    }

    @Override
    public void onTaskCompleted(String result) {
        statusBar.setStatus(result);
    }

    @Override
    public void onDataLoaded(List<AnswerEvent> data) {
        dataTable.setData(data);
        statusBar.setStatus("Загружено " + data.size() + " событий");
        statusBar.setSuccessIconVisible(true);
    }

    @Override
    public void addEvent(AnswerEvent event) {
        dataTable.addEvent(event);
    }

    @Override
    public void clearTable() {
        dataTable.clearData();
    }

    @Override
    public void onServerSwitchFailed(String failedServerName, String currentServerName) {
        SwingUtilities.invokeLater(() -> {
            for (JRadioButtonMenuItem item : serverMenuItems) {
                if (item.getText().equals(currentServerName)) {
                    item.setSelected(true);
                    break;
                }
            }
        });
    }
}