package org.company.spacedrepetitionbot.exception;

import java.io.IOException;

public class FileProcessingException extends RuntimeException {
    public FileProcessingException(String message, IOException e) {
        super(message, e);
    }
}
