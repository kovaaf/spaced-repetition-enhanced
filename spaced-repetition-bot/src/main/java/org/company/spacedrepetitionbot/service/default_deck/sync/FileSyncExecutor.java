package org.company.spacedrepetitionbot.service.default_deck.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.default_deck.parsing.MarkdownCardFileProcessor;
import org.company.spacedrepetitionbot.service.default_deck.utility.PathService;
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
        String relativePath = pathService.absolutePathToRelativePathString(filePath);

        if (Files.exists(filePath)) {
            fileProcessor.processFile(deck, filePath);
        } else {
            log.warn("File not found: {}", filePath);
            // Мягкое удаление всех карточек этого файла
            cardService.softDeleteByDeckAndSourceFilePath(deck, relativePath);
        }
        processedFiles.add(relativePath);
    }
}