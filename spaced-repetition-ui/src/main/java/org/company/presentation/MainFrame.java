package org.company.presentation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.application.FilterController;
import org.company.domain.AnswerEvent;
import org.company.domain.GrpcDataService;
import org.company.presentation.components.DataTable;
import org.company.presentation.components.FilterPanel;
import org.company.presentation.components.LogPanel;
import org.company.presentation.components.StatusBar;
import org.company.presentation.theme.ThemeManager;
import org.company.presentation.view.TaskView;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@Slf4j
public class MainFrame extends JFrame implements TaskView {
    private final FilterController filterController;
    private final DataTable dataTable;
    private final StatusBar statusBar;
    @Getter
    private final LogPanel logPanel;
    private final JSplitPane splitPane;
    private boolean logsVisible = false;
    private int lastDividerLocation = 0;

    // Компоненты для меню тем
    private ButtonGroup themeGroup;
    private JRadioButtonMenuItem lightThemeItem;
    private JRadioButtonMenuItem darkThemeItem;

    public MainFrame(FilterController filterController) {
        this.filterController = filterController;
        this.dataTable = new DataTable();
        this.statusBar = new StatusBar(this);
        this.logPanel = new LogPanel();

        JScrollPane tableScroll = new JScrollPane(dataTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Данные"));
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, null);
        splitPane.setResizeWeight(1.0);

        initUI();
        createMenuBar(); // Добавляем меню

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Останавливаем текущую загрузку и стриминг (если активны)
                filterController.cancelLoading();

                // Закрываем gRPC канал в отдельном потоке, чтобы не блокировать EDT
                if (filterController.getDataService() instanceof GrpcDataService) {
                    new Thread(() -> {
                        try {
                            ((GrpcDataService) filterController.getDataService()).shutdown();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }, "grpc-shutdown-thread").start();
                }
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
        JMenu themeMenu = new JMenu("Theme");

        themeGroup = new ButtonGroup();

        // Создаём пункты для светлой и тёмной темы
        lightThemeItem = new JRadioButtonMenuItem("Light");
        darkThemeItem = new JRadioButtonMenuItem("Dark");

        lightThemeItem.addActionListener(e -> {
            if (lightThemeItem.isSelected()) {
                ThemeManager.applyTheme("Light");
            }
        });

        darkThemeItem.addActionListener(e -> {
            if (darkThemeItem.isSelected()) {
                ThemeManager.applyTheme("Dark");
            }
        });

        themeGroup.add(lightThemeItem);
        themeGroup.add(darkThemeItem);
        themeMenu.add(lightThemeItem);
        themeMenu.add(darkThemeItem);

        menuBar.add(themeMenu);
        setJMenuBar(menuBar);

        // Устанавливаем выбранный пункт в соответствии с текущей темой
        updateSelectedTheme();
    }

    private void updateSelectedTheme() {
        // Определяем текущую тему по имени LookAndFeel
        LookAndFeel current = UIManager.getLookAndFeel();
        String name = current.getName();
        if (name.contains("Dark") || name.contains("dark")) {
            darkThemeItem.setSelected(true);
        } else {
            lightThemeItem.setSelected(true);
        }
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
}