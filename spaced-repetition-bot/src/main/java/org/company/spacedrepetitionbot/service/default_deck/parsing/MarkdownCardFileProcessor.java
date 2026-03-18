package org.company.spacedrepetitionbot.service.default_deck.parsing;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.exception.FileProcessingException;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.utility.PathService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CardFileProcessor} that processes Markdown files.
 * <p>
 * Parses a Markdown file into cards using {@link MarkdownParser} and updates the database accordingly:
 * creates new cards, updates existing ones, and deletes obsolete cards for the given source file.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class MarkdownCardFileProcessor implements CardFileProcessor {
    private final CardService cardService;
    private final DeckService deckService;
    private final MarkdownParser markdownParser;
    private final PathService pathService;

    @Override
    public List<Card> parseMarkdownFile(Path file) {
        try {
            String content = Files.readString(file);
            String relativePath = pathService.absolutePathToRelativePathString(file);
            return markdownParser.parseMarkdown(content, relativePath);
        } catch (IOException e) {
            throw new FileProcessingException("Error parsing file: " + file, e);
        }
    }

    @Override
    public void processFile(Deck deck, Path filePath) {
        String relativePath = pathService.absolutePathToRelativePathString(filePath);
        List<Card> cards = parseMarkdownFile(filePath);

        cards.forEach(card -> updateOrCreateCard(deck, card, relativePath));
        deleteObsoleteCards(deck, relativePath, cards);
    }

    /**
     * Updates an existing card or creates a new one if it does not exist.
     *
     * @param deck         the deck
     * @param card         the card data from the file
     * @param relativePath the relative path of the source file
     */
    private void updateOrCreateCard(Deck deck, Card card, String relativePath) {
        cardService.getBySourceFilePathAndSourceHeading(relativePath, card.getSourceHeading()).ifPresentOrElse(
                existing -> {
                    if (!existing.getFront().equals(card.getFront()) || !existing.getBack().equals(card.getBack())) {
                        updateCard(existing, card);
                    }
                }, () -> createCard(card, deck));
    }

    private void updateCard(Card existing, Card newData) {
        existing.setFront(newData.getFront());
        existing.setBack(newData.getBack());
        cardService.save(existing);
    }

    /**
     * Creates a new card and associates it with the deck.
     *
     * @param card the card to create
     * @param deck the deck
     */
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

    /**
     * Deletes cards from the database that are no longer present in the file.
     *
     * @param deck         the deck
     * @param relativePath the source file path
     * @param cards        the current list of cards from the file
     */
    private void deleteObsoleteCards(Deck deck, String relativePath, List<Card> cards) {
        List<String> validFronts = cards.stream().map(Card::getFront).collect(Collectors.toList());
        cardService.deleteByDeckAndSourceFilePathAndFrontNotIn(deck, relativePath, validFronts);
    }
}
