package org.company.ui.exception;

/**
 * Exception thrown when data retrieval from the remote service fails.
 * Wraps underlying I/O or gRPC errors.
 */
public class DataServiceException extends Exception {
    public DataServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}