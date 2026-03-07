package org.company.presentation.components;

import javax.swing.*;
import java.awt.*;

public class LogPanel extends JScrollPane {
    private final JTextArea logArea = new JTextArea(8, 40);

    public LogPanel() {
        super();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        // Явно устанавливаем цвета из текущей темы
        logArea.setBackground(UIManager.getColor("TextArea.background"));
        logArea.setForeground(UIManager.getColor("TextArea.foreground"));
        setViewportView(logArea);
        setBorder(BorderFactory.createTitledBorder("Логи приложения"));
    }

    public void append(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> logArea.setText(""));
    }
}