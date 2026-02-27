package org.company.spacedrepetition.ui.integration;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.edt.GuiQuery;
import org.assertj.swing.fixture.FrameFixture;
import org.company.spacedrepetition.ui.client.AnalyticsServiceClient;
import org.company.spacedrepetition.ui.frame.MainFrame;
import org.company.spacedrepetition.ui.logic.StatisticsDataFetcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * Base class for UI integration tests.
 * Sets up a MainFrame with mocked AnalyticsServiceClient and provides AssertJ Swing fixtures.
 */
public abstract class UITestBase {
    
    protected FrameFixture window;
    protected MainFrame mainFrame;
    protected AnalyticsServiceClient mockClient;
    protected StatisticsDataFetcher mockDataFetcher;
    
    @BeforeEach
    public void setUp() {
        // Create mock client and data fetcher
        mockClient = mock(AnalyticsServiceClient.class);
        mockDataFetcher = new StatisticsDataFetcher(mockClient);
        // Stub getUsers to return placeholder users for UserFilter
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.UsersResponse usersResponse = 
            org.company.spacedrepetitiondata.grpc.AnalyticsProto.UsersResponse.newBuilder()
                .addUsers(org.company.spacedrepetitiondata.grpc.AnalyticsProto.User.newBuilder()
                    .setId(1).setName("User 1").build())
                .addUsers(org.company.spacedrepetitiondata.grpc.AnalyticsProto.User.newBuilder()
                    .setId(2).setName("User 2").build())
                .build();
        when(mockClient.getUsers()).thenReturn(usersResponse);
        // Stub getAnalytics to return empty response by default to avoid NPEs in tests
        when(mockClient.getAnalytics(any(), any(), any())).thenReturn(
                org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnalyticsResponse.newBuilder()
                        .setTotalCount(0).build());
        
        // Create MainFrame on EDT and inject mock data fetcher via reflection
        mainFrame = GuiActionRunner.execute(new GuiQuery<MainFrame>() {
            @Override
            protected MainFrame executeInEDT() {
                MainFrame frame = new MainFrame();
                injectMockDataFetcher(frame, mockDataFetcher);
                // Load users using mock data fetcher (which uses mock client)
                frame.getUserFilter().loadUsers(mockDataFetcher);
                stopAutoRefreshTimer(frame); // prevent timer from interfering with tests
                assignComponentNames(frame); // assign names for testability
                return frame;
            }
        });
        
        // Create robot and frame fixture
        Robot robot = BasicRobot.robotWithCurrentAwtHierarchy();
        window = new FrameFixture(robot, mainFrame);
        window.show(); // make the frame visible
        // Wait for UI to be fully visible and ready
        window.robot().waitForIdle();
    }
    
    protected void injectMockDataFetcher(MainFrame frame, StatisticsDataFetcher dataFetcher) {
        try {
            Field dataFetcherField = MainFrame.class.getDeclaredField("dataFetcher");
            dataFetcherField.setAccessible(true);
            dataFetcherField.set(frame, dataFetcher);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject mock data fetcher into MainFrame", e);
        }
    }
    
    private void stopAutoRefreshTimer(MainFrame frame) {
        try {
            Field timerField = MainFrame.class.getDeclaredField("autoRefreshTimer");
            timerField.setAccessible(true);
            javax.swing.Timer timer = (javax.swing.Timer) timerField.get(frame);
            if (timer != null) {
                timer.stop();
            }
        } catch (NoSuchFieldException e) {
            // Timer field removed - ignore silently
            return;
        } catch (IllegalAccessException e) {
            // Should not happen since we set accessible, but ignore
            return;
        }
    }
    
    /**
     * Waits for a JComboBox to contain a specific item text.
     * Polls the combo box items until the item is found or timeout expires.
     *
     * @param comboBoxName the name of the JComboBox component
     * @param itemText the text of the item to wait for
     * @param timeoutMs maximum time to wait in milliseconds
     */
    protected void waitForComboBoxToContainItem(String comboBoxName, String itemText, int timeoutMs) {
        long endTime = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endTime) {
            try {
                JComboBox<?> comboBox = window.comboBox(comboBoxName).target();
                for (int i = 0; i < comboBox.getItemCount(); i++) {
                    Object item = comboBox.getItemAt(i);
                    if (item != null && item.toString().equals(itemText)) {
                        return;
                    }
                }
            } catch (Exception e) {
                // Component might not be found yet, continue waiting
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // If we exit loop without finding item, test will fail later with LocationUnavailableException
    }
    
    private void assignComponentNames(MainFrame frame) {
        assignNamesRecursively(frame.getContentPane());
    }
    
    private void assignNamesRecursively(Container container) {
        for (Component comp : container.getComponents()) {
            // Assign name based on component's text if name is null
            if (comp instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) comp;
            if (button.getName() == null && button.getText() != null && !button.getText().isEmpty()) {
                    button.setName(button.getText());
                }
            } else if (comp instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) comp;
                combo.setName("UserFilterComboBox");
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
            if (label.getName() == null && label.getText() != null && label.getText().contains("Ready")) {
                    label.setName("StatusLabel");
                }
            } else if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                table.setName("StatisticsTable");
            }
            // Recurse into containers
            if (comp instanceof Container) {
                assignNamesRecursively((Container) comp);
            }
        }
    }
    
    @AfterEach
    public void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }
}