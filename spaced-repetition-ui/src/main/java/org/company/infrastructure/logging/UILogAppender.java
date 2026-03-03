package org.company.infrastructure.logging;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.company.presentation.components.LogPanel;

import javax.swing.*;
import java.io.Serializable;

public class UILogAppender extends AbstractAppender {
    private final LogPanel logPanel;

    public UILogAppender(String name, LogPanel logPanel) {
        this(name, null, PatternLayout.newBuilder()
                .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                .build(), true, logPanel);
    }

    protected UILogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, LogPanel logPanel) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        this.logPanel = logPanel;
    }

    @Override
    public void append(LogEvent event) {
        if (logPanel != null) {
            String message = new String(getLayout().toByteArray(event));
            SwingUtilities.invokeLater(() -> logPanel.append(message));
        }
    }
}