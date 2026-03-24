package org.company.ui.infrastructure.ui.swing.presenter;

import lombok.extern.slf4j.Slf4j;
import org.company.ui.application.port.output.grpc.StreamingListener;
import org.company.ui.application.port.output.ui.TaskView;
import org.company.ui.application.usecase.LoadDataUseCase;
import org.company.ui.application.usecase.StreamingExecutorUseCase;
import org.company.ui.domain.entity.AnswerEvent;
import org.company.ui.domain.entity.TimeFilter;
import org.company.ui.exception.DataServiceException;

import javax.swing.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Presenter for the filtering and streaming functionality.
 * Orchestrates data loading and streaming, updates the view, and handles cancellation.
 */
@Slf4j
public class FilterPresenter {
    private final LoadDataUseCase loadDataUseCase;
    private final StreamingExecutorUseCase streamingExecutorUseCase;
    private final TaskView view;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SwingWorker<List<AnswerEvent>, Void> currentWorker;
    private Instant lastLoadedTimestamp;

    public FilterPresenter(LoadDataUseCase loadDataUseCase, StreamingExecutorUseCase streamingExecutorUseCase, TaskView view) {
        this.loadDataUseCase = loadDataUseCase;
        this.streamingExecutorUseCase = streamingExecutorUseCase;
        this.view = view;
    }

    /**
     * Called when a time filter button is pressed.
     * Cancels any ongoing operations, clears the table, loads historical data,
     * and then starts streaming from the most recent loaded event (or a fallback time).
     *
     * @param filter the selected time range
     */
    public void onFilterSelected(TimeFilter filter) {
        cancelCurrentOperations();

        view.setRunningState(true);
        view.setProgressIndeterminate(true);
        view.setStatus("Загрузка данных...");

        currentWorker = new SwingWorker<>() {
            @Override
            protected List<AnswerEvent> doInBackground() throws DataServiceException {
                return loadDataUseCase.execute(filter);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        view.onTaskCancelled();
                    } else {
                        List<AnswerEvent> result = get();
                        view.clearTable(); // всегда очищаем таблицу перед новыми данными

                        if (result != null && !result.isEmpty()) {
                            view.onDataLoaded(result);
                            view.onTaskCompleted("Загружено " + result.size() + " событий");
                            lastLoadedTimestamp = result.stream()
                                    .map(AnswerEvent::timestamp)
                                    .max(Instant::compareTo)
                                    .orElse(null);
                        } else {
                            // Пустой результат – не ошибка, просто нет данных
                            view.onTaskCompleted("Нет данных за выбранный период");
                            lastLoadedTimestamp = null;
                        }

                        // Запускаем стриминг с соответствующей начальной точки
                        Instant streamStart = lastLoadedTimestamp != null
                                ? lastLoadedTimestamp
                                : calculateFallbackStart(filter);
                        streamingExecutorUseCase.startStreaming(streamStart, new StreamingListener() {
                            @Override
                            public void onEvent(AnswerEvent event) {
                                if (lastLoadedTimestamp != null && !event.timestamp().isAfter(lastLoadedTimestamp)) {
                                    return; // пропускаем дубликаты
                                }
                                SwingUtilities.invokeLater(() -> view.addEvent(event));
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("Streaming error", error);
                            }

                            @Override
                            public void onCompleted() {
                                log.info("Streaming completed");
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    view.showError("Загрузка прервана");
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof DataServiceException) {
                        view.showError("Ошибка при загрузке данных с сервера. Проверьте соединение.");
                    } else {
                        view.showError("Неизвестная ошибка при загрузке данных.");
                    }
                    log.error("Error during data loading", e);
                } finally {
                    view.setProgressIndeterminate(false);
                    view.setRunningState(false);
                }
            }
        };
        currentWorker.execute();
    }

    /**
     * Cancels any ongoing data loading and stops the stream.
     */
    public void cancelLoading() {
        cancelCurrentOperations();
    }

    private void cancelCurrentOperations() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
        streamingExecutorUseCase.stopStreaming();
    }

    private Instant calculateFallbackStart(TimeFilter filter) {
        Instant now = Instant.now();
        return switch (filter) {
            case LAST_DAY -> now.minusSeconds(24 * 60 * 60);
            case LAST_WEEK -> now.minusSeconds(7 * 24 * 60 * 60);
            case LAST_MONTH -> now.minusSeconds(30 * 24 * 60 * 60);
            case LAST_YEAR -> now.minusSeconds(365 * 24 * 60 * 60);
            case ALL_TIME -> Instant.EPOCH;
        };
    }

    /**
     * Shuts down the internal executor and stops all operations.
     * Called when the application is closing.
     */
    public void shutdown() {
        executor.shutdownNow();
        cancelCurrentOperations();
    }
}