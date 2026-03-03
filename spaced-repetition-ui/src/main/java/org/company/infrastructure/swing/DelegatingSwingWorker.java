package org.company.infrastructure.swing;

import lombok.extern.slf4j.Slf4j;
import org.company.domain.Task;
import org.company.domain.TaskContext;

import javax.swing.*;

@Slf4j
public class DelegatingSwingWorker<T, V> extends SwingWorker<T, V> {
    private final Task<T> task;

    public DelegatingSwingWorker(Task<T> task) {
        this.task = task;
    }

    @Override
    protected T doInBackground() throws Exception {
        log.debug("doInBackground начат в потоке {}", Thread.currentThread().getName());

        TaskContext context = new TaskContext() {
            @Override
            public boolean isCancelled() {
                return DelegatingSwingWorker.this.isCancelled();
            }

            @Override
            public void setProgress(int progress) {
                DelegatingSwingWorker.this.setProgress(progress);
            }
        };

        return task.execute(context);
    }
}