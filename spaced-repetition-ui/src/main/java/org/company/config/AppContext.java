package org.company.config;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.company.config.logging.UILogAppender;
import org.company.domain.ServerInfo;
import org.company.domain.TimeFilter;
import org.company.presentation.MainFrame;
import org.company.presentation.presenter.FilterPresenter;
import org.company.presentation.presenter.ServerPresenter;
import org.company.service.dao.*;
import org.company.service.usecases.LoadDataUseCase;
import org.company.service.usecases.ServerSwitchListener;
import org.company.service.usecases.StreamingUseCase;
import org.company.service.usecases.SwitchServerUseCase;
import org.company.service.utility.ServerConnectionChecker;

import javax.swing.*;
import java.util.List;

/**
 * Application context that wires all components together and starts the UI.
 * This is the composition root of the application.
 */
@Slf4j
public class AppContext {

    /**
     * Creates and displays the main application window.
     * It initializes configuration, infrastructure, use cases, presenters,
     * and sets up the log appender.
     */
    public static void createAndShowMainFrame() {
        configureLookAndFeel();

        AppProperties props = loadConfiguration();
        List<ServerInfo> servers = props.getServers();
        String defaultUrl = props.getDefaultServerUrl();

        InfrastructureComponents infra = createDataSourceComponents(defaultUrl);
        UseCases useCases = createUseCases(infra, servers, defaultUrl);

        MainFrame mainFrame = createMainFrame(servers, defaultUrl);
        setupLogAppender(mainFrame);

        Presenters presenters = createAndWirePresenters(useCases, mainFrame);
        mainFrame.setFilterPresenter(presenters.filterPresenter);
        mainFrame.setServerPresenter(presenters.serverPresenter);
        mainFrame.ensureFilterPanel();

        addServerSwitchListener(useCases.switchServerUseCase, useCases.loadDataUseCase, useCases.streamingUseCase);

        mainFrame.setVisible(true);
        log.info("Приложение запущено");

        triggerInitialLoad(presenters.filterPresenter);
    }

    private static void configureLookAndFeel() {
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
    }

    private static AppProperties loadConfiguration() {
        return new AppProperties();
    }

    private static InfrastructureComponents createDataSourceComponents(String defaultUrl) {
        DataServiceFactory dataServiceFactory = new DataServiceFactory();
        ServerConnectionChecker connectionChecker = new ServerConnectionChecker();

        DataService defaultDataService = dataServiceFactory.create(defaultUrl);
        if (!(defaultDataService instanceof GrpcDataService)) {
            throw new IllegalStateException("Expected GrpcDataService");
        }
        GrpcDataService grpcDataService = (GrpcDataService) defaultDataService;
        StreamingService streamingService = new GrpcStreamingService(grpcDataService);

        return new InfrastructureComponents(
                dataServiceFactory,
                connectionChecker,
                defaultDataService,
                streamingService
        );
    }

    private static UseCases createUseCases(InfrastructureComponents infra,
            List<ServerInfo> servers,
            String defaultUrl) {
        LoadDataUseCase loadDataUseCase = new LoadDataUseCase(infra.defaultDataService);
        StreamingUseCase streamingUseCase = new StreamingUseCase(infra.streamingService);
        SwitchServerUseCase switchServerUseCase = new SwitchServerUseCase(
                servers, infra.connectionChecker, infra.dataServiceFactory, defaultUrl);

        return new UseCases(loadDataUseCase, streamingUseCase, switchServerUseCase);
    }

    private static MainFrame createMainFrame(List<ServerInfo> servers, String defaultUrl) {
        return new MainFrame(servers, defaultUrl);
    }

    private static void setupLogAppender(MainFrame mainFrame) {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        UILogAppender appender = new UILogAppender("UILogAppender", mainFrame.getLogPanel());
        appender.start();
        config.addAppender(appender);
        LoggerConfig rootLogger = config.getRootLogger();
        rootLogger.addAppender(appender, null, null);
        rootLogger.setLevel(Level.INFO);
        context.updateLoggers();
    }

    private static Presenters createAndWirePresenters(UseCases useCases, MainFrame mainFrame) {
        FilterPresenter filterPresenter = new FilterPresenter(
                useCases.loadDataUseCase,
                useCases.streamingUseCase,
                mainFrame
        );
        ServerPresenter serverPresenter = new ServerPresenter(
                useCases.switchServerUseCase,
                mainFrame,
                () -> SwingUtilities.invokeLater(() -> filterPresenter.onFilterSelected(TimeFilter.LAST_DAY))
        );
        return new Presenters(filterPresenter, serverPresenter);
    }

    private static void addServerSwitchListener(SwitchServerUseCase switchServerUseCase,
            LoadDataUseCase loadDataUseCase,
            StreamingUseCase streamingUseCase) {
        switchServerUseCase.addListener(new ServerSwitchListener() {
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
    }

    private static void triggerInitialLoad(FilterPresenter filterPresenter) {
        SwingUtilities.invokeLater(() -> filterPresenter.onFilterSelected(TimeFilter.LAST_DAY));
    }

    private record InfrastructureComponents(
            DataServiceFactory dataServiceFactory,
            ServerConnectionChecker connectionChecker,
            DataService defaultDataService,
            StreamingService streamingService
    ) {}

    private record UseCases(
            LoadDataUseCase loadDataUseCase,
            StreamingUseCase streamingUseCase,
            SwitchServerUseCase switchServerUseCase
    ) {}

    private record Presenters(
            FilterPresenter filterPresenter,
            ServerPresenter serverPresenter
    ) {}
}