package org.company.application;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.*;
import org.company.infrastructure.swing.DelegatingSwingWorker;
import org.company.presentation.view.TaskView;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
public class FilterController {
    @Getter
    private final DataService dataService;
    @Setter
    private TaskView view;
    private SwingWorker<List<AnswerEvent>, String> worker;
    private volatile Runnable stopStreaming;
    private TimeFilter currentFilter;

    public FilterController(DataService dataService) {
        this.dataService = dataService;
    }

    public void applyFilter(TimeFilter filter) {
        if (view == null) {
            throw new IllegalStateException("TaskView not set. Call setView() before using controller.");
        }
        log.info("Applying filter: {}", filter); // добавлено
        stopStreaming();

        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }

        view.setRunningState(true);
        view.setProgressIndeterminate(true);
        view.setStatus("Загрузка данных...");

        currentFilter = filter;
        DataLoadingTask task = new DataLoadingTask(dataService, filter);

        worker = new DelegatingSwingWorker<>(task) {
            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        view.onTaskCancelled();
                    } else {
                        List<AnswerEvent> result = get();
                        if (result != null) {
                            view.onDataLoaded(result);
                            view.onTaskCompleted("Данные загружены");
                            startStreaming();
                        } else {
                            view.showError("Не удалось загрузить данные");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    view.showError("Прервано");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    view.showError("Ошибка: " + cause.getMessage());
                } finally {
                    view.setProgressIndeterminate(false);
                    view.setRunningState(false);
                }
            }
        };

        worker.execute();
    }

    private void startStreaming() {
        if (!(dataService instanceof GrpcDataService grpcService)) {
            log.debug("DataService is not gRPC, streaming disabled");
            return;
        }
        log.info("Starting streaming for filter: {}", currentFilter);
        stopStreaming = grpcService.startStreaming(currentFilter,
                event -> {
                    log.debug("New event received, adding to table");
                    SwingUtilities.invokeLater(() -> view.addEvent(event));
                },
                () -> log.warn("Stream error occurred"),
                () -> log.info("Stream completed"));
    }

    private void stopStreaming() {
        if (stopStreaming != null) {
            log.info("Stopping current stream");
            stopStreaming.run();
            stopStreaming = null;
        }
    }

    public void cancelLoading() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        stopStreaming();
    }
}