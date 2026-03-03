package org.company.domain.exception;

public class StreamingException extends RuntimeException {
    public StreamingException(String message) {
        super(message);
    }
    public StreamingException(String message, Throwable cause) {
        super(message, cause);
    }
}