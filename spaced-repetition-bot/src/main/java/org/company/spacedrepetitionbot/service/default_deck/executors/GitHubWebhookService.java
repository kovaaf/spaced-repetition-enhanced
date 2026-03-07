package org.company.spacedrepetitionbot.service.default_deck.executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.AppProperties;
import org.company.spacedrepetitionbot.dto.WebhookPayload;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.ChangedFilesProcessor;
import org.company.spacedrepetitionbot.service.default_deck.RepoUrlNormalizer;
import org.company.spacedrepetitionbot.service.default_deck.WebhookValidator;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.processors.SyncEventProcessor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class GitHubWebhookService {
    private static final String REF_PREFIX = "refs/heads/";
    private static final String PUSH_EVENT = "push";
    private final AppProperties appProperties;
    private final RepoUrlNormalizer repoUrlNormalizer;
    private final ChangedFilesProcessor changedFilesProcessor;
    private final DeckService deckService;
    private final WebhookValidator webhookValidator;
    private final SyncEventProcessor syncEventProcessor;
    private final ObjectMapper objectMapper;

    public void processWebhook(String event, String signature, String rawPayload) throws IOException {
        WebhookPayload payload = null;
        try {
            payload = objectMapper.readValue(rawPayload, WebhookPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing payload", e);
            throw new IOException(e);
        }
        webhookValidator.validateSignature(rawPayload, signature); // Валидация подписи
        handleEvent(event, payload);
    }

    private void handleEvent(String event, WebhookPayload payload) {
        AppProperties.DefaultDeckConfig defaultDeck = appProperties.getDefaultDeck();

        if (!isExpectedRepoAndBranch(payload, defaultDeck)) {
            log.info("Webhook ignored for repository: {}, branch: {}", payload.repository().fullName(), payload.ref());
            return;
        }

        if (PUSH_EVENT.equals(event)) {
            handlePushEvent(payload, defaultDeck);
        } else {
            log.info("Unhandled event type: {}", event);
        }
    }

    private void handlePushEvent(WebhookPayload payload, AppProperties.DefaultDeckConfig deckConfig) {
        Deck deck = deckService.getDeckByName(deckConfig.getName());
        List<String> changedFiles = changedFilesProcessor.getChangedFiles(payload, deckConfig);

        syncEventProcessor.processSyncEvent(new SyncEventDTO(deck.getDeckId(), false, changedFiles));
    }

    private boolean isExpectedRepoAndBranch(WebhookPayload payload, AppProperties.DefaultDeckConfig defaultDeck) {
        String expectedRepo = repoUrlNormalizer.normalize(defaultDeck.getRepo().getUrl());
        String expectedBranch = REF_PREFIX + defaultDeck.getRepo().getBranch();

        return expectedRepo.equals(payload.repository().fullName()) && expectedBranch.equals(payload.ref());
    }
}
