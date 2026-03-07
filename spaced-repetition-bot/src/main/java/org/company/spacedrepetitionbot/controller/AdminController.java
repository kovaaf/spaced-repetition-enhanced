package org.company.spacedrepetitionbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.processors.SyncEventProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {
    private final SyncEventProcessor syncEventProcessor;
    private final DeckService deckService;

    @GetMapping("/force-sync")
    public ResponseEntity<String> forceSync() {

        Long deckId = deckService.getDefaultDeck().getDeckId();
        syncEventProcessor.processSyncEvent(new SyncEventDTO(deckId, true, null));
        return ResponseEntity.ok("Sync queued successfully");
    }
}
