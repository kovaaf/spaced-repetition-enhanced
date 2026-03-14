package org.company.application.usecase;

import org.company.domain.ServerInfo;
import org.company.infrastructure.data.DataService;

public interface ServerSwitchListener {
    void onServerSwitched(ServerInfo previous, ServerInfo newServer, DataService newDataService);
    void onServerSwitchFailed(String failedServerName, String currentServerName);
}