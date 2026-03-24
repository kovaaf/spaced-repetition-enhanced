package org.company.ui.application.port.output.ui;

import org.company.ui.domain.entity.AnswerEvent;

import java.util.List;

/**
 * View interface for the main application window.
 * Defines all operations that the UI must support.
 */
public interface TaskView {
    void setRunningState(boolean running);
    void setProgressIndeterminate(boolean indeterminate);
    void setStatus(String status);
    void showError(String message);
    void onTaskCancelled();
    void onTaskCompleted(String result);
    void onDataLoaded(List<AnswerEvent> data);
    void addEvent(AnswerEvent event);
    void clearTable();
    void onServerSwitchFailed(String failedServerName, String currentServerName);
}