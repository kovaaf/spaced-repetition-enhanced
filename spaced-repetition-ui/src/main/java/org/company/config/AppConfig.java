package org.company.config;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.company.application.FilterController;
import org.company.domain.TimeFilter;
import org.company.infrastructure.logging.UILogAppender;
import org.company.presentation.MainFrame;

import javax.swing.*;
import java.util.List;

@Slf4j
public class AppConfig {

    public static void createAndShowMainFrame() {
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

        AppProperties props = new AppProperties();
        List<ServerInfo> servers = props.getServers();
        String defaultUrl = props.getDefaultServerUrl();

        // Создаём контроллер с временным сервисом (будет заменён в ServerManager)
        FilterController filterController = new FilterController(null);

        MainFrame mainFrame = new MainFrame(filterController, servers, defaultUrl);
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

        // Загружаем данные при старте (после того как ServerManager установил соединение)
        SwingUtilities.invokeLater(() -> filterController.applyFilter(TimeFilter.LAST_DAY));
    }
}