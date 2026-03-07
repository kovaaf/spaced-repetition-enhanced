package org.company.presentation.components;

import org.company.presentation.MainFrame;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {
    private final JLabel statusLabel = new JLabel("Готов");
    private final JLabel successIcon = new JLabel("✓");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JButton cancelButton = new JButton("Cancel");
    private final JToggleButton logButton = new JToggleButton("Show Logs");

    public StatusBar(MainFrame mainFrame) {
        setLayout(new BorderLayout(5, 0));

        // Левая часть: иконка успеха и текст статуса
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftPanel.setOpaque(false);
        successIcon.setFont(new Font("SansSerif", Font.BOLD, 16));
        successIcon.setForeground(Color.GREEN);
        successIcon.setVisible(false);
        leftPanel.add(Box.createHorizontalStrut(3));
        leftPanel.add(statusLabel);
        leftPanel.add(Box.createHorizontalStrut(3));
        leftPanel.add(successIcon);
        leftPanel.add(Box.createHorizontalStrut(3));
        add(leftPanel, BorderLayout.WEST);

        // Центральная часть: прогресс-бар и кнопка Cancel
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        centerPanel.setOpaque(false);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        cancelButton.setVisible(false);
        centerPanel.add(progressBar);
        centerPanel.add(cancelButton);
        add(centerPanel, BorderLayout.CENTER);

        // Правая часть: кнопка логов
        add(logButton, BorderLayout.EAST);

        cancelButton.addActionListener(e -> mainFrame.cancelLoading());
        logButton.addActionListener(e -> mainFrame.toggleLogs());
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setProgressVisible(boolean visible) {
        progressBar.setVisible(visible);
        cancelButton.setVisible(visible); // кнопка Cancel видна только при загрузке
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }

    public void setLogButtonText(boolean logsVisible) {
        logButton.setText(logsVisible ? "Hide Logs" : "Show Logs");
    }

    public void setSuccessIconVisible(boolean visible) {
        successIcon.setVisible(visible);
        revalidate();
        repaint();
    }
}