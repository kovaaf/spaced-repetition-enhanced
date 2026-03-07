package org.company.presentation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.application.FilterController;
import org.company.application.ServerManager;
import org.company.config.ServerInfo;
import org.company.domain.AnswerEvent;
import org.company.presentation.components.DataTable;
import org.company.presentation.components.FilterPanel;
import org.company.presentation.components.LogPanel;
import org.company.presentation.components.StatusBar;
import org.company.presentation.theme.ThemeManager;
import org.company.presentation.view.TaskView;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MainFrame extends JFrame implements TaskView {
    private final FilterController filterController;
    private final ServerManager serverManager;
    private final DataTable dataTable;
    private final StatusBar statusBar;
    @Getter
    private final LogPanel logPanel;
    private final JSplitPane splitPane;
    private boolean logsVisible = false;
    private int lastDividerLocation = 0;

    private ButtonGroup themeGroup;
    private ButtonGroup serverGroup;
    private List<JRadioButtonMenuItem> themeItems = new ArrayList<>();
    private List<JRadioButtonMenuItem> serverItems = new ArrayList<>();

    public MainFrame(FilterController filterController, List<ServerInfo> servers, String defaultServerUrl) {
        this.filterController = filterController;
        this.dataTable = new DataTable();
        this.statusBar = new StatusBar(this);
        this.logPanel = new LogPanel();
        this.serverManager = new ServerManager(servers, filterController, this, defaultServerUrl);

        JScrollPane tableScroll = new JScrollPane(dataTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Данные"));
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, null);
        splitPane.setResizeWeight(1.0);

        initUI();
        createMenuBar();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                serverManager.shutdown();
            }
        });
    }

    private void initUI() {
        setTitle("Data Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        FilterPanel filterPanel = new FilterPanel(filterController);
        add(filterPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setSize(1024, 720);
        setLocationRelativeTo(null);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Меню Theme
        JMenu themeMenu = new JMenu("Theme");
        themeGroup = new ButtonGroup();
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
            themeItems.add(item);
            if (name.equals("Dark")) {
                item.setSelected(true);
            }
        }
        menuBar.add(themeMenu);

        // Меню Source Server
        JMenu serverMenu = new JMenu("Source Server");
        serverGroup = new ButtonGroup();
        List<ServerInfo> servers = serverManager.getServers();
        String currentUrl = serverManager.getCurrentServerUrl();

        for (ServerInfo server : servers) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(server.name());
            item.addActionListener(e -> {
                if (item.isSelected()) {
                    serverManager.switchToServer(server.name());
                }
            });
            serverGroup.add(item);
            serverMenu.add(item);
            serverItems.add(item);
            if (server.url().equals(currentUrl)) {
                item.setSelected(true);
            }
        }
        menuBar.add(serverMenu);

        setJMenuBar(menuBar);
    }

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

    public void cancelLoading() {
        filterController.cancelLoading();
    }

    // === Реализация TaskView ===
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
        // Возвращаем выделение в меню на текущий сервер
        SwingUtilities.invokeLater(() -> {
            for (JRadioButtonMenuItem item : serverItems) {
                if (item.getText().equals(currentServerName)) {
                    item.setSelected(true);
                    break;
                }
            }
        });
    }
}