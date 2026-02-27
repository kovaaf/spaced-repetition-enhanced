package org.company.spacedrepetition.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * UI Component for displaying application logs in a scrollable text area.
 */
public class LogsPanel extends JPanel {
    private final JTextArea logTextArea;
    private final JScrollPane scrollPane;
    private static final int MAX_ROWS = 1000;

    public LogsPanel() {
        setName("logsPanel");
        setLayout(new BorderLayout());
        
        logTextArea = new JTextArea();
        logTextArea.setName("logsTextArea");
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        add(scrollPane, BorderLayout.CENTER);
        
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                "Application Logs"
        ));
    }

    public synchronized void appendLog(String text) {
        logTextArea.append(text);
        trimLog();
        // Auto-scroll to bottom
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
    }

    private void trimLog() {
        int lineCount = logTextArea.getLineCount();
        if (lineCount > MAX_ROWS) {
            try {
                int endOffset = logTextArea.getLineEndOffset(lineCount - MAX_ROWS - 1);
                logTextArea.replaceRange("", 0, endOffset);
            } catch (Exception e) {
                // Ignore trimming errors
            }
        }
    }

    public void clear() {
        logTextArea.setText("");
    }
}
