package org.company.service.usecases;

import org.company.domain.ServerInfo;
import org.company.service.dao.DataService;

/**
 * Listener for server switch events.
 * Implementations are called from the thread that performs the switch,
 * so they should delegate UI updates to the Event Dispatch Thread if needed.
 */
public interface ServerSwitchListener {

    /**
     * Called after a successful server switch.
     *
     * @param previous       the previously active server
     * @param newServer      the newly activated server
     * @param newDataService the {@link DataService} instance for the new server
     */
    void onServerSwitched(ServerInfo previous, ServerInfo newServer, DataService newDataService);

    /**
     * Called when a server switch attempt fails (e.g., connection refused).
     *
     * @param failedServerName   the name of the server that could not be reached
     * @param currentServerName the name of the server that remains active
     */
    void onServerSwitchFailed(String failedServerName, String currentServerName);
}