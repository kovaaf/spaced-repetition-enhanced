package org.company.spacedrepetitionbot.service.default_deck;

import org.commonmark.node.*;
import org.company.spacedrepetitionbot.model.Card;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CardVisitor extends AbstractVisitor {
    private final List<Card> cards = new ArrayList<>();
    private final StringBuilder currentContent = new StringBuilder();
    private final String filePath;
    private final String fileName;
    private boolean beforeFirstHeading = true;
    private String currentHeading = null;
    private int listLevel = 0; // Track nesting level of lists

    public CardVisitor(String filePath) {
        this.filePath = filePath;
        this.fileName = getFileNameWithoutExtension(filePath);
    }

    @Override
    public void visit(Heading heading) {
        saveCurrentCard(); // Всегда сохраняем предыдущую карточку

        currentHeading = extractPlainText(heading);
        currentContent.setLength(0);
        beforeFirstHeading = false;
    }

    @Override
    public void visit(Text text) {
        currentContent.append(text.getLiteral());
        super.visit(text);
    }

    @Override
    public void visit(FencedCodeBlock code) {
        currentContent.append("```").append(code.getInfo()).append("\n").append(code.getLiteral()).append("```\n");
        super.visit(code);
    }

    @Override
    public void visit(Code code) {
        currentContent.append("`").append(code.getLiteral()).append("`");
    }

    @Override
    public void visit(Paragraph paragraph) {
        super.visit(paragraph);
        currentContent.append("\n");
    }

    @Override
    public void visit(ListItem listItem) {
        Node parent = listItem.getParent();
        if (parent instanceof BulletList) {
            currentContent.append(getIndent()).append("- ");
        }
        super.visit(listItem);
    }

    @Override
    public void visit(BulletList bulletList) {
        listLevel++;
        super.visit(bulletList);
        listLevel--;
    }

    @Override
    public void visit(OrderedList orderedList) {
        listLevel++;
        int counter = orderedList.getMarkerStartNumber();
        for (Node child = orderedList.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof ListItem) {
                currentContent.append(getIndent()).append(counter).append(". ");
                visit((ListItem) child);
                counter++;
            }
        }
        listLevel--;
    }

    private String getIndent() {
        return "  ".repeat(listLevel - 1);
    }

    private void saveInitialContent() {
        String content = currentContent.toString().trim();
        if (!content.isEmpty()) {
            cards.add(Card.builder().front(fileName).back(content).sourceFilePath(filePath).build());
        }
        currentContent.setLength(0);
    }

    public List<Card> getCards() {
        if (beforeFirstHeading) {
            saveInitialContent();
        } else {
            saveCurrentCard();
        }
        return new ArrayList<>(cards);
    }

    private void saveCurrentCard() {
        if (currentHeading != null || beforeFirstHeading) {
            String content = currentContent.toString().trim();
            if (!content.isEmpty()) {
                // Для карточек до первого заголовка используем только имя файла
                // Для карточек на основе заголовка добавляем префикс имени файла
                String front = beforeFirstHeading ? fileName : fileName + ". " + currentHeading;
                cards.add(Card.builder()
                        .front(front)
                        .back(content)
                        .sourceFilePath(filePath)
                        .sourceHeading(currentHeading)
                        .build());
            }
            currentContent.setLength(0);
            currentHeading = null;
        }
    }

    private String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text) {
                sb.append(((Text) child).getLiteral());
            } else if (child instanceof Code) {
                sb.append("`").append(((Code) child).getLiteral()).append("`");
            } else if (child instanceof Link) {
                sb.append(extractLinkText((Link) child));
            } else if (child instanceof Emphasis) {
                sb.append("*").append(extractText(child)).append("*");
            } else if (child instanceof StrongEmphasis) {
                sb.append("**").append(extractText(child)).append("**");
            } else {
                sb.append(extractText(child));
            }
        }
        return sb.toString();
    }

    private String extractPlainText(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text) {
                sb.append(((Text) child).getLiteral());
            } else if (child instanceof Code) {
                sb.append(((Code) child).getLiteral());
            } else if (child instanceof Link) {
                sb.append(extractPlainText(child));
            } else {
                sb.append(extractPlainText(child));
            }
        }
        return sb.toString().trim();
    }

    private String extractLinkText(Link link) {
        StringBuilder sb = new StringBuilder();
        for (Node child = link.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text) {
                sb.append(((Text) child).getLiteral());
            } else {
                sb.append(extractText(child));
            }
        }
        return sb.toString();
    }

    private String getFileNameWithoutExtension(String filePath) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            fileName = fileName.substring(0, dotIndex);
        }
        return fileName;
    }
}
