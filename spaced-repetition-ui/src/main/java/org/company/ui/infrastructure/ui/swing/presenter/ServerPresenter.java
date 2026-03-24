package org.company.ui.infrastructure.ui.swing.presenter;

import lombok.extern.slf4j.Slf4j;
import org.company.ui.application.port.output.grpc.DataService;
import org.company.ui.application.port.output.grpc.ServerSwitchListener;
import org.company.ui.application.port.output.ui.TaskView;
import org.company.ui.application.usecase.SwitchServerUseCase;
import org.company.ui.domain.entity.ServerInfo;

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