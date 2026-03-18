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

/**
 * Executor that processes a single file during synchronization.
 * <p>
 * If the file exists, it delegates to {@link MarkdownCardFileProcessor} to update cards.
 * If the file does not exist, all cards belonging to that source file are deleted.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSyncExecutor {
    private final MarkdownCardFileProcessor fileProcessor;
    private final CardService cardService;
    private final PathService pathService;

    /**
     * Processes a single file: updates or deletes cards accordingly.
     *
     * @param deck           the deck to which the file belongs
     * @param filePath       the absolute path of the file
     * @param processedFiles a set to which the relative path of the file will be added (for tracking)
     */
    public void processFile(Deck deck, Path filePath, Set<String> processedFiles) {
        String relativePath = pathService.absolutePathToRelativePathString(filePath);

        if (Files.exists(filePath)) {
            fileProcessor.processFile(deck, filePath);
        } else {
            log.warn("File not found: {}", filePath);
            cardService.deleteByDeckAndSourceFilePath(deck, relativePath);
        }
        processedFiles.add(relativePath);
    }
}
