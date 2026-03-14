package org.company.domain;

public interface StreamingListener {
    void onEvent(AnswerEvent event);
    void onError(Throwable error);
    void onCompleted();
}
