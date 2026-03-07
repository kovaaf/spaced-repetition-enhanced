package org.company.spacedrepetitionbot.exception;

public class DeckNotFoundException extends RuntimeException {
    public DeckNotFoundException(String message) {
        super(message);
    }
}
