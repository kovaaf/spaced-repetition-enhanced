package org.company.spacedrepetitionbot.utils;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownEscaper {

    public String escapeMarkdownV2(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Разделяем на code blocks и обычный текст
        Pattern pattern = Pattern.compile("(```[\\s\\S]*?```)|(`[^`]*`)|([^`]+)");
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Code block - оставляем как есть
                result.append(matcher.group(1));
            } else if (matcher.group(2) != null) {
                // Inline code - оставляем как есть
                result.append(matcher.group(2));
            } else {
                // Обычный текст - экранируем
                result.append(escapeAllSpecialCharsMarkdownV2(matcher.group(3)));
            }
        }

        return result.toString();
    }

    private String escapeAllSpecialCharsMarkdownV2(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Экранируем все специальные символы MarkdownV2
        return text.replace("\\", "\\\\")  // сначала экранируем обратные слеши
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}