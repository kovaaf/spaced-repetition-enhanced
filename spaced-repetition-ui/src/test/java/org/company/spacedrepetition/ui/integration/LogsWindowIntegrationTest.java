package org.company.spacedrepetition.ui.integration;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import javax.swing.JPanel;
import static org.assertj.core.api.Assertions.assertThat;

public class LogsWindowIntegrationTest extends UITestBase {
    private static final Logger LOG = LoggerFactory.getLogger(LogsWindowIntegrationTest.class);

    @Test
    public void testToggleLogsVisibility() {
        // Initially logsPanel should be hidden (as per MainFrame.java)
        JPanel logsPanelComp = window.robot().finder().findByName(mainFrame, "logsPanel", JPanel.class, false);
        assertThat(logsPanelComp.isVisible()).isFalse();
        window.button("toggleLogsButton").requireText("Show Logs");

        // Click "Show Logs"
        window.button("toggleLogsButton").click();
        window.robot().waitForIdle();
        assertThat(logsPanelComp.isVisible()).isTrue();
        window.button("toggleLogsButton").requireText("Hide Logs");

        // Click "Hide Logs"
        window.button("toggleLogsButton").click();
        window.robot().waitForIdle();
        assertThat(logsPanelComp.isVisible()).isFalse();
        window.button("toggleLogsButton").requireText("Show Logs");
    }

    @Test
    public void testLogCapture() {
        // Show logs window
        window.button("toggleLogsButton").click();
        
        // Log a test message
        String testMessage = "INTEGRATION_TEST_LOG_MESSAGE_" + System.currentTimeMillis();
        LOG.info(testMessage);
        
        // Wait for UI update (SwingUtilities.invokeLater)
        window.robot().waitForIdle();
        
        // Verify logsTextArea contains the message
        javax.swing.text.JTextComponent logsTextArea = window.robot().finder().findByName(mainFrame, "logsTextArea", javax.swing.text.JTextComponent.class, false);
        assertThat(logsTextArea.getText()).contains(testMessage);
    }

    @Test
    public void testThreadSafetyLogging() throws InterruptedException {
        // Show logs window
        window.button("toggleLogsButton").click();

        // Log from a background thread
        String threadMessage = "BACKGROUND_THREAD_LOG_" + System.currentTimeMillis();
        Thread thread = new Thread(() -> {
            LOG.info(threadMessage);
        });
        thread.start();
        thread.join();

        // Wait for UI update
        window.robot().waitForIdle();

        // Verify logsTextArea contains the message
        javax.swing.text.JTextComponent logsTextArea = window.robot().finder().findByName(mainFrame, "logsTextArea", javax.swing.text.JTextComponent.class, false);
        assertThat(logsTextArea.getText()).contains(threadMessage);
    }
}
