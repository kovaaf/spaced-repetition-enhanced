package org.company.ui.infrastructure.ui.swing.components;

import javax.swing.*;
import java.awt.*;

/**
 * Scrollable panel that displays log messages in a monospaced text area.
 * Messages are appended and automatically scrolled to the bottom.
 */
public class LogPanel extends JScrollPane {
    private final JTextArea logArea = new JTextArea(8, 40);

    public LogPanel() {
        super();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(UIManager.getColor("TextArea.background"));
        logArea.setForeground(UIManager.getColor("TextArea.foreground"));
        setViewportView(logArea);
        setBorder(BorderFactory.createTitledBorder("Логи приложения"));
    }

    /**
     * Appends a line of text to the log area.
     * This method is thread‑safe; it uses {@link SwingUtilities#invokeLater}.
     *
     * @param line the text to append
     */
    public void append(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}