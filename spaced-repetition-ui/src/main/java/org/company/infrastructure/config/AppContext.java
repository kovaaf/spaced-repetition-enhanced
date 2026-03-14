package org.company.infrastructure.config;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.company.application.usecase.LoadDataUseCase;
import org.company.application.usecase.StreamingUseCase;
import org.company.application.usecase.SwitchServerUseCase;
import org.company.domain.ServerInfo;
import org.company.domain.StreamingService;
import org.company.domain.TimeFilter;
import org.company.infrastructure.data.*;
import org.company.infrastructure.logging.UILogAppender;
import org.company.presentation.MainFrame;
import org.company.presentation.presenter.FilterPresenter;
import org.company.presentation.presenter.ServerPresenter;

import javax.swing.*;
import java.util.List;

@Slf4j
public class AppContext {

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

        DataServiceFactory dataServiceFactory = new DataServiceFactory();
        ServerConnectionChecker connectionChecker = new ServerConnectionChecker();

        DataService defaultDataService = dataServiceFactory.create(defaultUrl);
        if (!(defaultDataService instanceof GrpcDataService)) {
            throw new IllegalStateException("Expected GrpcDataService");
        }
        GrpcDataService grpcDataService = (GrpcDataService) defaultDataService;
        StreamingService streamingService = new GrpcStreamingService(grpcDataService);

        LoadDataUseCase loadDataUseCase = new LoadDataUseCase(defaultDataService);
        StreamingUseCase streamingUseCase = new StreamingUseCase(streamingService);
        SwitchServerUseCase switchServerUseCase = new SwitchServerUseCase(
                servers, connectionChecker, dataServiceFactory, defaultUrl);

        MainFrame mainFrame = new MainFrame(servers, defaultUrl);

        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        UILogAppender appender = new UILogAppender("UILogAppender", mainFrame.getLogPanel());
        appender.start();
        config.addAppender(appender);
        LoggerConfig rootLogger = config.getRootLogger();
        rootLogger.addAppender(appender, null, null);
        rootLogger.setLevel(Level.INFO);
        context.updateLoggers();

        FilterPresenter filterPresenter = new FilterPresenter(loadDataUseCase, streamingUseCase, mainFrame);
        ServerPresenter serverPresenter = new ServerPresenter(switchServerUseCase, mainFrame,
                () -> SwingUtilities.invokeLater(() -> filterPresenter.onFilterSelected(TimeFilter.LAST_DAY)));

        mainFrame.setFilterPresenter(filterPresenter);
        mainFrame.setServerPresenter(serverPresenter);

        mainFrame.ensureFilterPanel();

        switchServerUseCase.addListener(new org.company.application.usecase.ServerSwitchListener() {
            @Override
            public void onServerSwitched(ServerInfo previous, ServerInfo newServer, DataService newDataService) {
                loadDataUseCase.setDataService(newDataService);
                if (newDataService instanceof GrpcDataService) {
                    streamingUseCase.setStreamingService(new GrpcStreamingService((GrpcDataService) newDataService));
                }
            }

            @Override
            public void onServerSwitchFailed(String failedServerName, String currentServerName) {
                // handled by presenter
            }
        });

        mainFrame.setVisible(true);
        log.info("Приложение запущено");

        SwingUtilities.invokeLater(() -> filterPresenter.onFilterSelected(TimeFilter.LAST_DAY));
    }
}