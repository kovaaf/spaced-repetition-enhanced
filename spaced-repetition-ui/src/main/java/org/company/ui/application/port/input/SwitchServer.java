package org.company.ui.application.port.input;

import org.company.ui.application.port.output.grpc.ServerSwitchListener;


public interface SwitchServer {
    void switchToServer(String serverName);
    void addListener(ServerSwitchListener listener);
    void removeListener(ServerSwitchListener listener);
}