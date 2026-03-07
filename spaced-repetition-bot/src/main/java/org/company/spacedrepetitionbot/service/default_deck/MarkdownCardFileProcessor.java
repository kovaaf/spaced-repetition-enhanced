package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.exception.FileProcessingException;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.DeckService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
            String relativePath = pathService.getRelativePath(file);
            return markdownParser.parseMarkdown(content, relativePath);
        } catch (IOException e) {
            throw new FileProcessingException("Error parsing file: " + file, e);
        }
    }

    @Override
    public void processFile(Deck deck, Path filePath) {
        String relativePath = pathService.getRelativePath(filePath);
        List<Card> cards = parseMarkdownFile(filePath);

        cards.forEach(card -> updateOrCreateCard(deck, card, relativePath));
        deleteObsoleteCards(deck, relativePath, cards);
    }

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

    private void deleteObsoleteCards(Deck deck, String relativePath, List<Card> cards) {
        List<String> validFronts = cards.stream().map(Card::getFront).collect(Collectors.toList());
        cardService.deleteByDeckAndSourceFilePathAndFrontNotIn(deck, relativePath, validFronts);
    }
}
