package org.company.spacedrepetitionbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.service.default_deck.executors.GitHubWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

// Для работы нужен статический адрес\домен - устанавливается в веб хуке на GitHub. Можно получить временный с помощью
// ngrok (для его работы нужен впн)
@Slf4j
@RestController
@RequestMapping("/webhook/github")
@RequiredArgsConstructor
public class GitHubWebhookController {
    private final GitHubWebhookService gitHubWebhookService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false)
            String event,
            @RequestHeader(value = "X-Hub-Signature", required = false)
            String signature,
            @RequestBody
            String rawPayload) {

        log.debug("Received GitHub webhook with event: {}", event);
        try {
            gitHubWebhookService.processWebhook(event, signature, rawPayload);
            log.info("Webhook processed successfully");
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Invalid payload");
        }
    }
}