package org.company.spacedrepetitionbot.service.default_deck.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.parsing.MarkdownCardFileProcessor;
import org.company.spacedrepetitionbot.service.default_deck.utility.PathService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Processes multiple files during synchronisation.
 * <p>
 * This class orchestrates the handling of either a list of changed files (incremental sync)
 * or all Markdown files in the source folders (full sync).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSyncProcessor {
    private final CardService cardService;
    private final AppProperties appProperties;
    private final DeckService deckService;
    private final PathService pathService;
    private final MarkdownCardFileProcessor markdownCardFileProcessor;
    private final FileSystemScanner fileSystemScanner;
    private final FileSyncExecutor fileSyncExecutor;
    private final FileConsistencyValidator fileConsistencyValidator;

    /**
     * Processes a list of changed files (incremental sync).
     *
     * @param deck         the deck being synced
     * @param changedPaths relative paths of files that have changed
     */
    public void processChangedFiles(Deck deck, List<String> changedPaths) {
        Path repoPath = pathService.getRepoAbsolutePath();
        Set<String> processedFiles = new HashSet<>();

        changedPaths.stream()
                .filter(path -> path.endsWith(".md"))
                .filter(path -> pathService.isFileIncluded(path, getSourceFolders(), getExcludeFolders()))
                .forEach(path -> fileSyncExecutor.processFile(deck, repoPath.resolve(path), processedFiles));

        fileConsistencyValidator.verifyAfterIncrementalSync(deck, processedFiles);
    }

    private List<String> getExcludeFolders() {
        return appProperties.getDefaultDeck().getRepo().getExcludeFolders();
    }

    private List<String> getSourceFolders() {
        return appProperties.getDefaultDeck().getRepo().getSourceFolders();
    }

    /**
     * Processes all Markdown files in the configured source folders (full sync).
     *
     * @param deck the deck being synced
     */
    public void processAllMarkdownFiles(Deck deck) {
        Set<String> processedFilePaths = new HashSet<>();
        List<String> filesWithNoCards = new ArrayList<>();

        getSourceFolderPaths().forEach(rootPath -> processFolder(deck, rootPath, processedFilePaths, filesWithNoCards));

        if (!filesWithNoCards.isEmpty()) {
            log.error("No cards were created for the following files: {}", String.join(", ", filesWithNoCards));
        }

        deleteCardsNotInSourceFolders(deck, processedFilePaths);
    }

    private List<Card> processMarkdownFile(Deck deck, Path file) {
        log.debug("Processing file: {}", file);
        List<Card> cards = markdownCardFileProcessor.parseMarkdownFile(file);
        String relativePath = pathService.absolutePathToRelativePathString(file);

        cards.forEach(card -> updateOrCreateCard(deck, card, relativePath));
        deleteObsoleteCards(deck, relativePath, cards);
        return cards;
    }

    private void updateOrCreateCard(Deck deck, Card card, String relativePath) {
        cardService.getBySourceFilePathAndSourceHeading(relativePath, card.getSourceHeading()).ifPresentOrElse(
                existing -> {
                    if (!existing.getFront().equals(card.getFront()) || !existing.getBack().equals(card.getBack())) {
                        updateCard(existing, card);
                    }
                }, () -> createCard(card, deck));
    }

    private void deleteObsoleteCards(Deck deck, String relativePath, List<Card> cards) {
        List<String> validFronts = cards.stream().map(Card::getFront).collect(Collectors.toList());
        cardService.deleteByDeckAndSourceFilePathAndFrontNotIn(deck, relativePath, validFronts);
    }

    private void updateCard(Card existing, Card newData) {
        existing.setFront(newData.getFront());
        existing.setBack(newData.getBack());
        cardService.save(existing);
    }

    private void createCard(Card card, Deck deck) {
        if (deck.getDeckId() == null) {
            deck = deckService.save(deck);
        }

        if (deck.isDefault()) {
            card.setOriginalCardId(null);
        }

        card.setDeck(deck);
        cardService.save(card);
    }

    private List<Path> getSourceFolderPaths() {
        Path repoPath = Paths.get(appProperties.getDefaultDeck().getRepo().getPath()).toAbsolutePath();
        List<String> sourceFolders = getSourceFolders();

        if (sourceFolders == null || sourceFolders.isEmpty()) {
            return List.of(repoPath);
        }

        return sourceFolders.stream().map(repoPath::resolve).collect(Collectors.toList());
    }

    private void processFolder(
            Deck deck,
            Path rootPath,
            Set<String> processedFilePaths,
            List<String> filesWithNoCards) {
        if (!Files.exists(rootPath)) {
            log.warn("Source folder does not exist: {}", rootPath);
            return;
        }

        try {
            fileSystemScanner.findMarkdownFiles(rootPath).forEach(file -> {
                String relativePath = pathService.absolutePathToRelativePathString(file);
                if (!pathService.isFileIncluded(relativePath, getSourceFolders(), getExcludeFolders())) {
                    return;
                }
                try {
                    List<Card> cards = processMarkdownFile(deck, file);
                    processedFilePaths.add(relativePath);
                    if (cards.isEmpty()) {
                        filesWithNoCards.add(relativePath);
                    }
                } catch (Exception e) {
                    log.error("Error processing file: {}", file, e);
                }
            });
        } catch (IOException e) {
            log.error("Failed to process folder: {}", rootPath, e);
        }
    }

    private void deleteCardsNotInSourceFolders(Deck deck, Set<String> processedFilePaths) {
        int deletedCount = cardService.deleteByDeckAndSourceFilePathNotIn(deck, processedFilePaths);
        if (deletedCount > 0) {
            log.debug("Deleted {} obsolete cards", deletedCount);
        }
    }
}
