package org.company.application;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.config.ServerInfo;
import org.company.domain.GrpcDataService;
import org.company.domain.TimeFilter;
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
                        // Проверяем соединение лёгким запросом
                        newService.fetchData(TimeFilter.LAST_DAY); // может бросить исключение
                    } catch (Exception e) {
                        log.error("Failed to connect to new server: {}", server.url(), e);
                        // Закрываем созданный, но неиспользуемый сервис
                        if (newService != null) {
                            try {
                                newService.shutdown();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        // Показываем сообщение об ошибке
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Не удалось подключиться к серверу " + serverName + ".\n" + e.getMessage(),
                                    "Ошибка подключения",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                        // Возвращаем выбор в меню на предыдущий сервер (это должно быть сделано в UI)
                        // Но так как мы не имеем ссылки на меню, лучше уведомить view о необходимости обновить выделение.
                        // Можно через callback или событие. Упростим: пусть view сам восстановит выделение.
                        // Для этого добавим метод notifyServerSwitchFailed(String failedServerName) в TaskView.
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