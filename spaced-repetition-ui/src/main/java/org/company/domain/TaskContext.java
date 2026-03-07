package org.company.domain;

public interface TaskContext {
    boolean isCancelled();
    void setProgress(int progress);
}