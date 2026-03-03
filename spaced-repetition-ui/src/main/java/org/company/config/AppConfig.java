package org.company.config;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.company.application.FilterController;
import org.company.domain.DataService;
import org.company.domain.GrpcDataService;
import org.company.domain.TimeFilter;
import org.company.infrastructure.logging.UILogAppender;
import org.company.presentation.MainFrame;

import javax.swing.*;

@Slf4j
public class AppConfig {

    public static void createAndShowMainFrame() {
        // Устанавливаем FlatDarkLaf по умолчанию
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            log.warn("Не удалось установить FlatDarkLaf, используется системная", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                log.warn("Не удалось установить системный Look & Feel", ex);
            }
        }

        // Загружаем конфигурацию
        AppProperties props = new AppProperties();
        DataService dataService = new GrpcDataService(props.getGrpcServerUrl());

        FilterController filterController = new FilterController(dataService);
        MainFrame mainFrame = new MainFrame(filterController);
        filterController.setView(mainFrame);

        // Настройка Log4j2 аппендера для UI
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        UILogAppender appender = new UILogAppender("UILogAppender", mainFrame.getLogPanel());
        appender.start();
        config.addAppender(appender);
        LoggerConfig rootLogger = config.getRootLogger();
        rootLogger.addAppender(appender, null, null);
        rootLogger.setLevel(Level.INFO);
        context.updateLoggers();

        mainFrame.setVisible(true);
        log.info("Приложение запущено");

        SwingUtilities.invokeLater(() -> filterController.applyFilter(TimeFilter.LAST_DAY));
    }
}