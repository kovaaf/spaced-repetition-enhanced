package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.CardService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileSyncExecutor {
    private final MarkdownCardFileProcessor fileProcessor;
    private final CardService cardService;
    private final PathService pathService;

    public void processFile(Deck deck, Path filePath, Set<String> processedFiles) {
        String relativePath = pathService.getRelativePath(filePath);

        if (Files.exists(filePath)) {
            fileProcessor.processFile(deck, filePath);
        } else {
            log.warn("File not found: {}", filePath);
            cardService.deleteByDeckAndSourceFilePath(deck, relativePath);
        }
        processedFiles.add(relativePath);
    }
}
