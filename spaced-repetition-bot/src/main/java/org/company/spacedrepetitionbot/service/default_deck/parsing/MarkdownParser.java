package org.company.spacedrepetitionbot.service.default_deck.parsing;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.company.spacedrepetitionbot.model.Card;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing Markdown content into a list of {@link Card} objects.
 * <p>
 * This parser removes preamble, inline tags, and non‑breaking spaces,
 * then uses CommonMark to build an AST and {@link CardVisitor} to extract cards.
 * </p>
 */
@Slf4j
@Service
public class MarkdownParser {
    // Регулярные выражения для удаления преамбулы
    private static final Pattern TAG_PATTERN = Pattern.compile("^(\\s*#\\w+\\s*)+.*$");
    private static final Pattern LINK_PATTERN = Pattern.compile("^(\\s*\\[\\[.+?]]\\s*)+.*$");

    /**
     * Parses the given Markdown content and returns a list of cards.
     *
     * @param content  the raw Markdown string
     * @param filePath the path of the source file (used for logging and for the visitor)
     * @return a list of {@link Card} objects extracted from the content
     */
    public List<Card> parseMarkdown(String content, String filePath) {
        content = removePreamble(content);
        content = removeInlineTags(content);
        content = content.replace('\u00A0', ' ');

        Parser parser = Parser.builder().build();
        Node document = parser.parse(content);
        CardVisitor visitor = new CardVisitor(filePath);
        document.accept(visitor);
        return visitor.getCards();
    }

    /**
     * Removes leading preamble (tags and links) from the content.
     *
     * @param content the raw content
     * @return content with preamble removed
     */
    private String removePreamble(String content) {
        List<String> lines = content.lines().toList();
        boolean[] preambleEnded = {false};
        return lines.stream().filter(line -> {
            if (preambleEnded[0]) {
                return true;
            }
            String trimmed = line.trim();
            if (TAG_PATTERN.matcher(trimmed).matches() || LINK_PATTERN.matcher(trimmed).matches()) {
                return false;
            }
            if (trimmed.matches("^#+\\s?\\S+.*") || !trimmed.isEmpty()) {
                preambleEnded[0] = true;
            }
            return preambleEnded[0];
        }).collect(Collectors.joining("\n"));
    }

    /**
     * Removes inline tags (e.g., #tag) and unwikilinks (e.g., [[link]]).
     *
     * @param content the content to clean
     * @return cleaned content
     */
    private String removeInlineTags(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String cleaned = content.replaceAll("(?<!`)#\\p{L}+\\b", "");
        cleaned = cleaned.replaceAll("(?<!`)\\[\\[(.+?)]](?!`)", "$1");
        return cleaned;
    }
}