package org.company.ui.infrastructure.config;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.company.ui.application.port.output.grpc.DataService;
import org.company.ui.application.port.output.grpc.ServerSwitchListener;
import org.company.ui.application.port.output.grpc.StreamingService;
import org.company.ui.application.usecase.LoadDataUseCase;
import org.company.ui.application.usecase.StreamingExecutorUseCase;
import org.company.ui.application.usecase.SwitchServerUseCase;
import org.company.ui.domain.entity.ServerInfo;
import org.company.ui.domain.entity.TimeFilter;
import org.company.ui.infrastructure.config.logging.UILogAppender;
import org.company.ui.infrastructure.config.properties.*;
import org.company.ui.infrastructure.config.properties.constants.ENV_CONSTANTS;
import org.company.ui.infrastructure.grpc.client.DataServiceFactory;
import org.company.ui.infrastructure.grpc.client.GrpcDataService;
import org.company.ui.infrastructure.grpc.client.GrpcStreamingService;
import org.company.ui.infrastructure.grpc.client.ServerConnectionChecker;
import org.company.ui.infrastructure.ui.swing.MainFrame;
import org.company.ui.infrastructure.ui.swing.presenter.FilterPresenter;
import org.company.ui.infrastructure.ui.swing.presenter.ServerPresenter;

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
        List<ServerInfo> servers = props.data().getServersInfo();
        String defaultUrl = determineDefaultUrl(props.data());;

        InfrastructureComponents infra = createDataSourceComponents(defaultUrl);
        UseCases useCases = createUseCases(infra, servers, defaultUrl);

        MainFrame mainFrame = createMainFrame(servers, defaultUrl);
        setupLogAppender(mainFrame);

        Presenters presenters = createAndWirePresenters(useCases, mainFrame);
        mainFrame.setFilterPresenter(presenters.filterPresenter);
        mainFrame.setServerPresenter(presenters.serverPresenter);
        mainFrame.ensureFilterPanel();

        addServerSwitchListener(useCases.switchServerUseCase, useCases.loadDataUseCase, useCases.streamingExecutorUseCase);

        mainFrame.setVisible(true);
        log.info("Приложение запущено");

        triggerInitialLoad(presenters.filterPresenter);
    }

    private static String determineDefaultUrl(DataProperties data) {
        // 1. Check environment variable
        String envDefault = EnvUtility.getEnv(String.valueOf(ENV_CONSTANTS.DATA_DEFAULT_SERVER_URL), null);
        if (envDefault != null) {
            log.info("Using default server URL from environment variable: {}", envDefault);
            return envDefault;
        }

        List<ServerProperties> serverProps = data.servers();
        if (serverProps == null || serverProps.isEmpty()) {
            throw new RuntimeException("No servers configured");
        }

        // 2. Look for server marked as default
        List<ServerProperties> defaultServers = serverProps.stream()
                .filter(ServerProperties::isDefault)
                .toList();

        if (defaultServers.size() > 1) {
            throw new RuntimeException("Multiple servers marked as default in configuration. Only one can be default.");
        }

        if (defaultServers.size() == 1) {
            log.info("Using default server marked in configuration: {}", defaultServers.get(0).url());
            return defaultServers.get(0).url();
        }

        // 3. Fallback to first server
        log.info("No default server specified, using first server: {}", serverProps.get(0).url());
        return serverProps.get(0).url();
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
        return AppPropertiesFactory.create();
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
        StreamingExecutorUseCase streamingExecutorUseCase = new StreamingExecutorUseCase(infra.streamingService);
        SwitchServerUseCase switchServerUseCase = new SwitchServerUseCase(
                servers, infra.connectionChecker, infra.dataServiceFactory, defaultUrl);

        return new UseCases(loadDataUseCase, streamingExecutorUseCase, switchServerUseCase);
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
                useCases.streamingExecutorUseCase,
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
            StreamingExecutorUseCase streamingExecutorUseCase) {
        switchServerUseCase.addListener(new ServerSwitchListener() {
            @Override
            public void onServerSwitched(ServerInfo previous, ServerInfo newServer, DataService newDataService) {
                loadDataUseCase.setDataService(newDataService);
                if (newDataService instanceof GrpcDataService) {
                    streamingExecutorUseCase.setStreamingService(new GrpcStreamingService((GrpcDataService) newDataService));
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
            StreamingExecutorUseCase streamingExecutorUseCase,
            SwitchServerUseCase switchServerUseCase
    ) {}

    private record Presenters(
            FilterPresenter filterPresenter,
            ServerPresenter serverPresenter
    ) {}
}