package org.company.presentation.presenter;

import lombok.extern.slf4j.Slf4j;
import org.company.domain.ServerInfo;
import org.company.presentation.TaskView;
import org.company.service.dao.DataService;
import org.company.service.usecases.ServerSwitchListener;
import org.company.service.usecases.SwitchServerUseCase;

import javax.swing.*;

/**
 * Presenter for server switching functionality.
 * Listens to server switch events and updates the view accordingly.
 */
@Slf4j
public class ServerPresenter implements ServerSwitchListener {
    private final SwitchServerUseCase switchServerUseCase;
    private final TaskView view;
    private final Runnable onServerChanged;

    public ServerPresenter(SwitchServerUseCase switchServerUseCase, TaskView view, Runnable onServerChanged) {
        this.switchServerUseCase = switchServerUseCase;
        this.view = view;
        this.onServerChanged = onServerChanged;
        this.switchServerUseCase.addListener(this);
    }

    /**
     * Initiates a switch to the server with the given name.
     *
     * @param serverName the name of the target server
     */
    public void onServerSelected(String serverName) {
        switchServerUseCase.switchToServer(serverName);
    }

    @Override
    public void onServerSwitched(ServerInfo previous, ServerInfo newServer, DataService newDataService) {
        SwingUtilities.invokeLater(() -> {
            view.clearTable();
            view.setStatus("Подключено к " + newServer.name());
            onServerChanged.run();
        });
    }

    @Override
    public void onServerSwitchFailed(String failedServerName, String currentServerName) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    null,
                    "Не удалось подключиться к серверу " + failedServerName + ".\nПроверьте сетевое соединение и доступность сервера.",
                    "Ошибка подключения",
                    JOptionPane.ERROR_MESSAGE
            );
            view.onServerSwitchFailed(failedServerName, currentServerName);
        });
    }
}