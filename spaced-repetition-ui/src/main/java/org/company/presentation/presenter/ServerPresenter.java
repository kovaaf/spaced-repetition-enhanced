package org.company.presentation.presenter;

import lombok.extern.slf4j.Slf4j;
import org.company.application.usecase.ServerSwitchListener;
import org.company.application.usecase.SwitchServerUseCase;
import org.company.domain.ServerInfo;
import org.company.infrastructure.data.DataService;
import org.company.presentation.view.TaskView;

import javax.swing.*;

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