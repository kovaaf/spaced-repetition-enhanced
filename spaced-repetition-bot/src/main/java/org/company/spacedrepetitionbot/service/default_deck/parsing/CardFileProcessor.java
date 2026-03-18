package org.company.spacedrepetitionbot.service.default_deck.parsing;

import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;

import java.nio.file.Path;
import java.util.List;

/**
 * Contract for processors that handle Markdown files and convert them into {@link Card} objects.
 * <p>
 * Implementations are responsible for parsing a Markdown file into a list of cards,
 * and for processing a given file to update the database accordingly.
 * </p>
 */
public interface CardFileProcessor {
    /**
     * Parses a Markdown file and returns a list of extracted cards.
     *
     * @param file the path to the Markdown file
     * @return a list of {@link Card} objects parsed from the file
     */
    List<Card> parseMarkdownFile(Path file);

    /**
     * Processes a Markdown file for a given deck: creates, updates, or deletes cards
     * in the database based on the file's content.
     *
     * @param deck     the deck to which the cards belong
     * @param filePath the path to the Markdown file
     */
    void processFile(Deck deck, Path filePath);
}
