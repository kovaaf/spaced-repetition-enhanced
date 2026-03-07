package org.company.spacedrepetitionbot.service.default_deck.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncEventDTO {
    private Long deckId;
    private boolean forceFullSync;
    private List<String> changedFiles;
}