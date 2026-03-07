package org.company.spacedrepetitionbot.controller_advice;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.exception.DeckNotFoundException;
import org.company.spacedrepetitionbot.exception.WebhookProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class WebhookExceptionHandler {

    @ExceptionHandler(DeckNotFoundException.class)
    public ResponseEntity<String> handleDeckNotFound(DeckNotFoundException ex) {
        log.error("Deck not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(WebhookProcessingException.class)
    public ResponseEntity<String> handleWebhookProcessingException(WebhookProcessingException ex) {
        log.error("Error processing webhook", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
    }
}
