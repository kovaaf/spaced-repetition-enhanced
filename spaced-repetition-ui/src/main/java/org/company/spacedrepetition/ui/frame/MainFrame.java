package org.company.spacedrepetition.ui.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableRowSorter;
import org.company.spacedrepetition.ui.client.AnalyticsServiceClient;
import org.company.spacedrepetition.ui.components.LogsPanel;
import org.company.spacedrepetition.ui.components.filters.PeriodFilter;
import org.company.spacedrepetition.ui.components.filters.UserFilter;
import org.company.spacedrepetition.ui.logic.LogCapture;
import org.company.spacedrepetition.ui.logic.NameCache;
import org.company.spacedrepetition.ui.logic.StatisticsDataFetcher;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;

/**
 * Main application frame for Spaced Repetition Statistics Viewer.
 * Follows TRD Appendix E layout specification with three main regions:
 * - NORTH: Filter panel (period, user)
 * - CENTER: Statistics table display
 * - SOUTH: Status bar
 * <p>
 * Uses BorderLayout as main layout manager with GridBagLayout for complex panel arrangements.
 */
public class MainFrame extends JFrame {

    private LogsPanel logsPanel;
    private JButton toggleLogsButton;
    private JPanel southContainer;
    private JPanel topPanel;
    private JPanel centerPanel;
    private JPanel bottomPanel;

    private PeriodFilter periodFilter;
    private UserFilter userFilter;
    private StatisticsDataFetcher dataFetcher;
    private JLabel statusLabel;
    private javax.swing.JProgressBar progressBar;
    // Streaming state tracking
    private boolean isStreamingActive = false;
    // Streaming support
    private AnalyticsServiceClient streamingClient;
    private JTable statisticsTable;
    private StatisticsTableModel tableModel;

    private javax.swing.Timer refreshTimer;

    /**
     * Constructs the main application frame with the specified layout.
     */
    public MainFrame() {
        logsPanel = new LogsPanel();
        System.setOut(new java.io.PrintStream(new LogCapture(System.out, logsPanel::appendLog)));
        System.setErr(new java.io.PrintStream(new LogCapture(System.err, logsPanel::appendLog)));
        initializeUI();
        initializeDataFetcher();
        setupListeners();
        // Streaming will be initialized after UI setup
        initializeStreaming();
    }

    /**
     * Initializes the user interface components and layout.
     */
    private void initializeUI() {
        // Set window title
        setTitle("Spaced Repetition Statistics Viewer");

        // Set window size
        setSize(1000, 700);

        // Set default close operation
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Add window listener to clean up resources on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });

        // Center window on screen
        setLocationRelativeTo(null);

        // Set minimum size
        setMinimumSize(new Dimension(800, 600));

        // Use BorderLayout as main layout manager
        setLayout(new BorderLayout(10, 10));

        // Create and add the three main panels
        createTopPanel();
        createCenterPanel();
        createBottomPanel();
        southContainer = new JPanel(new BorderLayout());
        southContainer.add(logsPanel, BorderLayout.CENTER);
        southContainer.add(bottomPanel, BorderLayout.SOUTH);
        logsPanel.setVisible(false);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(southContainer, BorderLayout.SOUTH);

        // Add some padding around the content
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private void initializeDataFetcher() {
        progressBar.setVisible(true);
        AnalyticsServiceClient client = AnalyticsServiceClient.createFromConfig();
        dataFetcher = new StatisticsDataFetcher(client);
        // Load real users asynchronously
        userFilter.loadUsers(dataFetcher);
        refreshData();
    }

    private void setupListeners() {
        // Add action listeners to filter components
        for (java.awt.Component comp : periodFilter.getComponents()) {
            if (comp instanceof javax.swing.JToggleButton) {
                ((javax.swing.JToggleButton) comp).addActionListener(e -> refreshData());
            }
        }
        userFilter.getComboBox().addActionListener(e -> refreshData());
    }

    private void refreshData() {
        if (dataFetcher == null) {
            return; // not initialized yet
        }
        String period = periodFilter.getSelectedPeriod();
        String userSelection = userFilter.getSelectedUser();

        // Convert period to time range
        Instant[] timeRange = StatisticsDataFetcher.periodToTimeRange(period);
        Instant startTime = timeRange[0];
        Instant endTime = timeRange[1];

        // Convert user selection to user ID
        String userId = StatisticsDataFetcher.userSelectionToUserId(userSelection);

        // Update status
        setStatus("Loading...");
        progressBar.setVisible(true);

        // Fetch data
        dataFetcher.fetchData(userId, startTime, endTime,
                response -> {
                // Success: update table on EDT
                SwingUtilities.invokeLater(() -> {
                    updateTableWithResponse(response);
                    setStatus("Ready - " + response.getTotalCount() + " events loaded");
                    // Only hide progress bar if streaming is not active
                    if (!isStreamingActive) {
                        progressBar.setVisible(false);
                    }
                });
            },
                exception -> {
                // Error: show error message on EDT
                SwingUtilities.invokeLater(() -> {
                    setStatus("Error: " + exception.getMessage());
                    // Only hide progress bar if streaming is not active
                    if (!isStreamingActive) {
                        progressBar.setVisible(false);
                    }
                });
            });
        restartStreaming();
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * Shows or hides the progress bar for streaming operations.
     * Called when streaming starts, stops, completes, or encounters errors.
     */
    private void setStreamingProgressBar(boolean visible) {
        isStreamingActive = visible;
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(visible);
        });
    }


    private void initializeStreaming() {
        // Create a separate client for streaming to avoid interference with regular requests
        streamingClient = AnalyticsServiceClient.createFromConfig();

        // Start streaming with current filter settings
        startStreaming();
    }

    private void startStreaming() {
        if (streamingClient == null) {
            return;
        }

        // Stream ALL events without filters for real-time updates
        // This ensures we receive all new events regardless of current filter settings
        // The UI will filter them locally based on current filter selection
        String userId = null; // null = all users
        Instant startTime = null; // null = no start time limit
        Instant endTime = null; // null = no end time limit
        // Start streaming
        streamingClient.streamAnalytics(
                userId,
                startTime,
                endTime,
                this::handleStreamingEvent,
                this::handleStreamingCompletion,
                this::handleStreamingError
        );

        // Show progress bar to indicate streaming is active
        setStreamingProgressBar(true);
        setStatus("Streaming started (all events)");
    }

    private void stopStreaming() {
        if (streamingClient != null) {
            try {
                streamingClient.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            streamingClient = null;
        }
        // Hide progress bar when streaming stops
        setStreamingProgressBar(false);
    }

    private void restartStreaming() {
        stopStreaming();
        // Small delay before restarting to avoid immediate reconnection
        new java.util.Timer().schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    startStreaming();
                }
            },
                100 // 100ms delay
        );
    }

    private void handleStreamingEvent(AnalyticsProto.AnswerEvent event) {
        // Update table with new event on EDT
        SwingUtilities.invokeLater(() -> {
            addEventToTable(event);
            // Update status to show streaming is active
            setStatus("Streaming active - new event received");
            // Ensure progress bar stays visible during streaming
            setStreamingProgressBar(true);
        });
    }

    private void handleStreamingCompletion() {
        // Stream completed (server closed connection)
        SwingUtilities.invokeLater(() -> {
            // Hide progress bar since streaming has completed
            setStreamingProgressBar(false);
            setStatus("Stream completed - attempting to reconnect");
            // Try to reconnect after a delay
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        startStreaming();
                    }
                },
                10000 // 10 second delay before attempting to restart streaming
            );
        });
    }

    private void handleStreamingError(Throwable error) {
        // Stream error occurred
        SwingUtilities.invokeLater(() -> {
            // Hide progress bar since streaming encountered an error
            setStreamingProgressBar(false);
            setStatus("Stream error: " + error.getMessage() + " - attempting to reconnect");
            // Try to restart streaming after a longer delay
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        startStreaming();
                    }
                },
                30000 // 30 second delay before attempting to restart streaming
            );
        });
    }

    private void addEventToTable(AnalyticsProto.AnswerEvent event) {
        if (tableModel == null) {
            return;
        }

        // Convert timestamp to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.getTimestamp().getSeconds()),
                ZoneId.systemDefault()
        );
        // Convert quality enum to readable string
        String qualityString = qualityToString(event.getQuality());
        NameCache cache = NameCache.getInstance();
        String userDisplay = cache.get(String.valueOf(event.getUserId()));
        if (userDisplay == null) {
            userDisplay = event.hasUserName() ? event.getUserName() : event.getUserId();
        }
        String deckDisplay = cache.get(String.valueOf(event.getDeckId()));
        if (deckDisplay == null) {
            deckDisplay = event.hasDeckName() ? event.getDeckName() : event.getDeckId();
        }
        String cardDisplay = cache.get(String.valueOf(event.getCardId()));
        if (cardDisplay == null) {
            cardDisplay = event.hasCardTitle() ? event.getCardTitle() : event.getCardId();
        }
        // Add row to table model
        // When using TableRowSorter, always use addRow() - the sorter will determine position
        tableModel.addRow(new Object[] {
                userDisplay,
                deckDisplay,
                cardDisplay,
                qualityString,
                dateTime  // LocalDateTime object, not formatted string
        });
        // Limit table size to prevent memory issues (keep last 1000 events)
        if (tableModel.getRowCount() > 1000) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
        }
    }

    /**
     * Creates the top panel for filters (period, user).
     * Uses GridBagLayout for precise component placement.
     */
    private void createTopPanel() {
        topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());

        // Add a titled border to indicate purpose
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                "Filters"
        );
        border.setTitleFont(new Font("Dialog", Font.BOLD, 12));
        border.setTitleColor(Color.DARK_GRAY);
        topPanel.setBorder(border);

        // Set preferred size for the top panel
        topPanel.setPreferredSize(new Dimension(0, 120));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Period filter label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel periodLabel = new JLabel("Period:");
        periodLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        topPanel.add(periodLabel, gbc);

        // Period filter component
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        periodFilter = new PeriodFilter();
        topPanel.add(periodFilter, gbc);

        // User filter label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel userLabel = new JLabel("User:");
        userLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        topPanel.add(userLabel, gbc);

        // User filter component
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        userFilter = new UserFilter();
        topPanel.add(userFilter, gbc);
    }

    /**
     * Creates the center panel for statistics table display.
     * Uses BorderLayout with a scrollable table.
     */
    private void createCenterPanel() {
        centerPanel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                "Statistics"
        );
        border.setTitleFont(new Font("Dialog", Font.BOLD, 12));
        border.setTitleColor(Color.DARK_GRAY);
        centerPanel.setBorder(border);
        String[] columnNames = {"User", "Deck", "Card", "Quality", "Date"};
        // Use StatisticsTableModel instead of DefaultTableModel for proper date sorting
        tableModel = new StatisticsTableModel(columnNames, 0);
        statisticsTable = new JTable(tableModel);
        statisticsTable.setFont(new Font("Dialog", Font.PLAIN, 12));
        statisticsTable.setRowHeight(25);
        statisticsTable.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));
        // Configure custom TableRowSorter with proper comparators
        configureTableSorter();

        // Set custom renderer for date column
        statisticsTable.setDefaultRenderer(LocalDateTime.class, new DateCellRenderer());
        // Add scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(statisticsTable);
        scrollPane.setPreferredSize(new Dimension(0, 0)); // Let BorderLayout manage size
        centerPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Configures the table sorter with proper comparators for all columns.
     * Sets up custom comparators for date column and configures default sorting
     * to show most recent events first.
     */
    private void configureTableSorter() {
        // Create custom TableRowSorter
        TableRowSorter<StatisticsTableModel> sorter = new TableRowSorter<>(tableModel);

        // Configure comparator for date column (column index 4)
        sorter.setComparator(4, new Comparator<LocalDateTime>() {
            @Override
            public int compare(LocalDateTime date1, LocalDateTime date2) {
                // Natural chronological ordering
                return date1.compareTo(date2);
            }
        });

        // Set default sorting: date column (index 4) in descending order (most recent first)
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);

        // Apply the sorter to the table
        statisticsTable.setRowSorter(sorter);
    }


    /**
     * Creates the bottom panel for status bar.
     * Uses BorderLayout with status on left.
     */
    private void createBottomPanel() {
        bottomPanel = new JPanel(new BorderLayout(10, 0));

        // Add a simple border
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Status label on the left
        statusLabel = new JLabel("Ready - Connected to data service: localhost:9091");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        
        progressBar = new javax.swing.JProgressBar();
        progressBar.setName("progressBar");
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 15));
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        
        toggleLogsButton = new JButton("Show Logs");
        toggleLogsButton.setName("toggleLogsButton");
        toggleLogsButton.setPreferredSize(new Dimension(toggleLogsButton.getPreferredSize().width, 15));
        toggleLogsButton.addActionListener(e -> {
            boolean visible = !logsPanel.isVisible();
            logsPanel.setVisible(visible);
            toggleLogsButton.setText(visible ? "Hide Logs" : "Show Logs");
            if (visible) {
                int height = (int) (getHeight() * 0.25);
                logsPanel.setPreferredSize(new Dimension(0, height));
            }
            revalidate();
            repaint();
        });
        bottomPanel.add(toggleLogsButton, BorderLayout.EAST);

        // Set preferred height for the bottom panel
        bottomPanel.setPreferredSize(new Dimension(0, 45));
    }

    /**
     * Gets the top panel (filters panel).
     * @return the top panel
     */
    public JPanel getTopPanel() {
        return topPanel;
    }

    /**
     * Gets the center panel (statistics table panel).
     * @return the center panel
     */
    public JPanel getCenterPanel() {
        return centerPanel;
    }

    /**
     * Gets the bottom panel (status panel).
     * @return the bottom panel
     */
    public JPanel getBottomPanel() {
        return bottomPanel;
    }

    /**
     * Gets the period filter component.
     * @return the period filter
     */
    public PeriodFilter getPeriodFilter() {
        return periodFilter;
    }

    /**
     * Gets the user filter component.
     * @return the user filter
     */
    public UserFilter getUserFilter() {
        return userFilter;
    }

    /**
     * Updates the statistics table with data from the analytics response.
     * Clears existing rows and adds new rows for each answer event.
     * Called on the EDT (Event Dispatch Thread).
     *
     * @param response the analytics response containing answer events
     */
    private void updateTableWithResponse(AnalyticsProto.AnalyticsResponse response) {
        if (tableModel == null || statisticsTable == null) {
            return; // UI not initialized
        }
        // Clear existing rows
        tableModel.setRowCount(0);
        for (AnalyticsProto.AnswerEvent event : response.getEventsList()) {
            // Convert timestamp to LocalDateTime
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(event.getTimestamp().getSeconds()),
                    ZoneId.systemDefault()
                );
            // Convert quality enum to readable string
            String qualityString = qualityToString(event.getQuality());
            NameCache cache = NameCache.getInstance();
            String userDisplay = cache.get(String.valueOf(event.getUserId()));
            if (userDisplay == null) {
                userDisplay = event.hasUserName() ? event.getUserName() : event.getUserId();
            }
            String deckDisplay = cache.get(String.valueOf(event.getDeckId()));
            if (deckDisplay == null) {
                deckDisplay = event.hasDeckName() ? event.getDeckName() : event.getDeckId();
            }
            String cardDisplay = cache.get(String.valueOf(event.getCardId()));
            if (cardDisplay == null) {
                cardDisplay = event.hasCardTitle() ? event.getCardTitle() : event.getCardId();
            }
            // Add row to table model
            // Store LocalDateTime object instead of formatted string
            tableModel.addRow(new Object[] {
                    userDisplay,
                    deckDisplay,
                    cardDisplay,
                    qualityString,
                    dateTime  // LocalDateTime object, not formatted string
            });
        }
    }

    /**
     * Converts Quality enum value to human-readable string.
     *
     * @param quality the quality enum value
     * @return string representation
     */
    private String qualityToString(AnalyticsProto.Quality quality) {
        return switch (quality) {
            case AGAIN -> "Again (0)";
            case HARD -> "Hard (3)";
            case GOOD -> "Good (4)";
            case EASY -> "Easy (5)";
            default -> "Unknown";
        };
    }

    /**
     * Cleans up resources before application exit.
     * Stops streaming client.
     */
    public void cleanup() {
        stopStreaming();
    }
}
