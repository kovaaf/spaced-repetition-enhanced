package org.company.application.usecase;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.ServerInfo;
import org.company.infrastructure.data.DataService;
import org.company.infrastructure.data.DataServiceFactory;
import org.company.infrastructure.data.ServerConnectionChecker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class SwitchServerUseCase {
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

    public void addListener(ServerSwitchListener listener) {
        listeners.add(listener);
    }

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