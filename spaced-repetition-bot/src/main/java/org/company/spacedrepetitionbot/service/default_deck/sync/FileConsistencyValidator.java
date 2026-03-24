package org.company.spacedrepetitionbot.service.default_deck.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.company.spacedrepetitionbot.exception.SyncException;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.default_deck.parsing.MarkdownCardFileProcessor;
import org.company.spacedrepetitionbot.service.default_deck.utility.PathService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validates consistency after an incremental sync.
 * <p>
 * Checks that each processed file has the expected number of cards,
 * and that the total card count matches the expected number for the current repository state.
 * If discrepancies are found, it attempts to correct them by reprocessing the file.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileConsistencyValidator {
    private final CardService cardService;
    private final MarkdownCardFileProcessor fileProcessor;
    private final FileSyncExecutor fileSyncExecutor;
    private final PathService pathService;
    private final AppProperties appProperties;

    /**
     * Verifies consistency after an incremental sync.
     *
     * @param deck           the deck that was synced
     * @param processedFiles set of relative file paths that were processed
     */
    public void verifyAfterIncrementalSync(Deck deck, Set<String> processedFiles) {
        if (processedFiles.isEmpty()) {
            return;
        }

        processedFiles.forEach(relativePath -> verifyFileConsistency(deck, relativePath));

        verifyTotalConsistency(deck);
    }

    private void verifyFileConsistency(Deck deck, String relativePath) {
        Path filePath = pathService.getRepoAbsolutePath().resolve(relativePath);
        int expected = fileProcessor.parseMarkdownFile(filePath).size();
        int actual = cardService.countByDeckAndSourceFilePath(deck, relativePath);

        if (actual < expected) {
            log.warn("Inconsistency detected in {}", relativePath);
            fileSyncExecutor.processFile(deck, filePath, new HashSet<>());
        }
    }

    private void verifyTotalConsistency(Deck deck) {
        int totalExpected = getExpectedCardCountForCurrentCommit();
        int totalActual = cardService.getActualCardCount(deck);

        if (totalActual < totalExpected) {
            log.warn("Global inconsistency detected. Expected: {}, actual: {}", totalExpected, totalActual);
            throw new SyncException("Data inconsistency detected");
        }
    }

    private int getExpectedCardCountForCurrentCommit() {
        return getSourceFolderPaths().stream().mapToInt(this::countCardsInFolder).sum();
    }

    private List<Path> getSourceFolderPaths() {
        Path repoPath = Paths.get(appProperties.getDefaultDeck().getRepo().getPath()).toAbsolutePath();
        List<String> sourceFolders = getSourceFolders();

        if (sourceFolders == null || sourceFolders.isEmpty()) {
            return List.of(repoPath);
        }

        return sourceFolders.stream().map(repoPath::resolve).collect(Collectors.toList());
    }

    private int countCardsInFolder(Path folderPath) {
        if (!Files.exists(folderPath)) {
            log.warn("Folder not found: {}", folderPath);
            return 0;
        }

        try (Stream<Path> walk = Files.walk(folderPath)) {
            return walk.filter(this::isMarkdownFile).filter((file -> {
                String relative = pathService.absolutePathToRelativePathString(file);
                return pathService.isFileIncluded(
                        relative,
                        getSourceFolders(),
                        getExcludeFolders()); // Фильтр исключений
            })).mapToInt(this::parseAndCountCards).sum();
        } catch (IOException e) {
            log.error("Error processing folder: {}", folderPath, e);
            return 0;
        }
    }

    private boolean isMarkdownFile(Path file) {
        return Files.isRegularFile(file) && file.toString().endsWith(".md");
    }

    private int parseAndCountCards(Path file) {
        return fileProcessor.parseMarkdownFile(file).size();
    }

    private List<String> getSourceFolders() {
        return appProperties.getDefaultDeck().getRepo().getSourceFolders();
    }

    private List<String> getExcludeFolders() {
        return appProperties.getDefaultDeck().getRepo().getExcludeFolders();
    }
}
