package org.company.domain.exception;

public class DataServiceException extends Exception {
    public DataServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}