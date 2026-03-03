package org.company.application;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.config.ServerInfo;
import org.company.domain.GrpcDataService;
import org.company.domain.TimeFilter;
import org.company.domain.exception.DataServiceException;
import org.company.presentation.view.TaskView;

import javax.swing.*;
import java.util.List;

@Slf4j
public class ServerManager {
    @Getter
    private final List<ServerInfo> servers;
    private final FilterController filterController;
    private final TaskView view;
    private GrpcDataService currentDataService;
    @Getter
    private String currentServerUrl;
    @Getter
    private String currentServerName;

    public ServerManager(List<ServerInfo> servers, FilterController filterController, TaskView view, String defaultUrl) {
        this.servers = servers;
        this.filterController = filterController;
        this.view = view;
        this.currentServerUrl = defaultUrl;
        this.currentServerName = findServerNameByUrl(defaultUrl);
        connectToServer(defaultUrl);
    }

    private String findServerNameByUrl(String url) {
        return servers.stream()
                .filter(s -> s.url().equals(url))
                .map(ServerInfo::name)
                .findFirst()
                .orElse("Unknown");
    }

    public void switchToServer(String serverName) {
        servers.stream()
                .filter(s -> s.name().equals(serverName))
                .findFirst()
                .ifPresent(server -> {
                    if (server.url().equals(currentServerUrl)) {
                        log.debug("Already connected to {}", serverName);
                        return;
                    }
                    log.info("Switching to server: {} ({})", serverName, server.url());

                    // Пытаемся создать новый сервис и проверить соединение
                    GrpcDataService newService = null;
                    try {
                        newService = new GrpcDataService(server.url());
                        newService.fetchData(TimeFilter.LAST_DAY); // проверка соединения
                    } catch (DataServiceException e) {
                        log.error("Failed to connect to new server {}: {}. Returning to previous server.", server.url(), e.getMessage(), e);
                        if (newService != null) {
                            try {
                                newService.shutdown();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Не удалось подключиться к серверу " + serverName + ".\nПроверьте сетевое соединение и доступность сервера.",
                                    "Ошибка подключения",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                        view.onServerSwitchFailed(serverName, currentServerName);
                        return;
                    } catch (Exception e) {
                        log.error("Unexpected error connecting to new server {}: {}", server.url(), e.getMessage(), e);
                        if (newService != null) {
                            try {
                                newService.shutdown();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Неизвестная ошибка при подключении к серверу " + serverName + ".\n" + e.getMessage(),
                                    "Ошибка подключения",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                        view.onServerSwitchFailed(serverName, currentServerName);
                        return;
                    }

                    // Закрываем старый сервис
                    if (currentDataService != null) {
                        try {
                            currentDataService.shutdown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // Очищаем таблицу
                    view.clearTable();

                    // Устанавливаем новый сервис
                    filterController.setDataService(newService);
                    this.currentDataService = newService;
                    this.currentServerUrl = server.url();
                    this.currentServerName = server.name();

                    // Загружаем данные с новым фильтром
                    filterController.applyFilter(TimeFilter.LAST_DAY);
                });
    }

    private void connectToServer(String url) {
        GrpcDataService newService = new GrpcDataService(url);
        filterController.setDataService(newService);
        this.currentDataService = newService;
        this.currentServerUrl = url;
        this.currentServerName = findServerNameByUrl(url);
    }

    public void shutdown() {
        if (currentDataService != null) {
            try {
                currentDataService.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}