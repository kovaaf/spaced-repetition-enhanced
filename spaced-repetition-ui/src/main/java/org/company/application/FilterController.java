package org.company.application;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.*;
import org.company.domain.exception.DataServiceException;
import org.company.infrastructure.swing.DelegatingSwingWorker;
import org.company.presentation.view.TaskView;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.Field;
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
                        log.info("Загрузка отменена пользователем");
                    } else {
                        List<AnswerEvent> result = get();
                        if (result != null) {
                            view.onDataLoaded(result);
                            view.onTaskCompleted("Данные загружены");
                            startStreaming();
                        } else {
                            view.showError("Не удалось загрузить данные (сервер вернул пустой результат)");
                            log.warn("Data loading returned null for filter {}", currentFilter);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    view.showError("Загрузка прервана");
                    log.warn("Data loading interrupted", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof DataServiceException) {
                        view.showError("Ошибка при загрузке данных с сервера. Проверьте соединение.");
                        log.error("Data service error during filter {}: {}", currentFilter, cause.getMessage(), cause);
                    } else if (cause instanceof IOException) {
                        view.showError("Сетевая ошибка. Проверьте соединение с сервером.");
                        log.error("Network error during filter {}: {}", currentFilter, cause.getMessage(), cause);
                    } else {
                        view.showError("Неизвестная ошибка при загрузке данных.");
                        log.error("Unexpected error during data loading for filter {}", currentFilter, cause);
                    }
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

    public void setDataService(DataService dataService) {
        // Останавливаем текущие операции
        cancelLoading();
        // Заменяем сервис
        try {
            Field field = FilterController.class.getDeclaredField("dataService");
            field.setAccessible(true);
            field.set(this, dataService);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed to set dataService", e);
        }
    }
}