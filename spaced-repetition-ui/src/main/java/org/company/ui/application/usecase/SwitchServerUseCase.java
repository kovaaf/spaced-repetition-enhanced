package org.company.ui.application.usecase;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.ui.application.port.input.SwitchServer;
import org.company.ui.application.port.output.grpc.DataService;
import org.company.ui.application.port.output.grpc.ServerSwitchListener;
import org.company.ui.domain.entity.ServerInfo;
import org.company.ui.infrastructure.grpc.client.DataServiceFactory;
import org.company.ui.infrastructure.grpc.client.ServerConnectionChecker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Use case for switching the active server.
 * Performs a connection check, creates a new {@link DataService} if successful,
 * and notifies all registered listeners about the change.
 */
@Slf4j
public class SwitchServerUseCase implements SwitchServer {
    private final List<ServerInfo> availableServers;
    private final ServerConnectionChecker connectionChecker;
    private final DataServiceFactory dataServiceFactory;
    private final List<ServerSwitchListener> listeners = new CopyOnWriteArrayList<>();

    @Getter
    private ServerInfo currentServer;

    public SwitchServerUseCase(List<ServerInfo> availableServers,
            ServerConnectionChecker connectionChecker,
            DataServiceFactory dataServiceFactory,
            String defaultServerUrl) {
        this.availableServers = availableServers;
        this.connectionChecker = connectionChecker;
        this.dataServiceFactory = dataServiceFactory;
        this.currentServer = findServerByUrl(defaultServerUrl)
                .orElseThrow(() -> new IllegalArgumentException("Default server not found"));
    }

    /**
     * Attempts to switch to the server with the given name.
     * If the switch succeeds, all listeners receive {@code onServerSwitched};
     * otherwise {@code onServerSwitchFailed} is called.
     *
     * @param serverName the name of the target server (must be in availableServers)
     */
    public void switchToServer(String serverName) {
        availableServers.stream()
                .filter(s -> s.name().equals(serverName))
                .findFirst()
                .ifPresent(server -> {
                    if (server.equals(currentServer)) {
                        log.debug("Already connected to {}", serverName);
                        return;
                    }
                    log.info("Switching to server: {} ({})", serverName, server.url());

                    boolean connected = connectionChecker.checkConnection(server.url());
                    if (!connected) {
                        notifyFailure(serverName, currentServer.name());
                    } else {
                        DataService newService = dataServiceFactory.create(server.url());
                        ServerInfo previous = currentServer;
                        currentServer = server;
                        notifySuccess(previous, server, newService);
                    }
                });
    }

    /**
     * Adds a listener that will be notified about server switch events.
     *
     * @param listener the listener to add
     */
    public void addListener(ServerSwitchListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously added listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(ServerSwitchListener listener) {
        listeners.remove(listener);
    }

    private void notifySuccess(ServerInfo previous, ServerInfo newServer, DataService newService) {
        listeners.forEach(l -> l.onServerSwitched(previous, newServer, newService));
    }

    private void notifyFailure(String failedServerName, String currentServerName) {
        listeners.forEach(l -> l.onServerSwitchFailed(failedServerName, currentServerName));
    }

    private java.util.Optional<ServerInfo> findServerByUrl(String url) {
        return availableServers.stream().filter(s -> s.url().equals(url)).findFirst();
    }
}