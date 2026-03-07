package org.company.spacedrepetitionbot.command.constant;

import lombok.Getter;

import java.util.Set;

@Getter
public enum Command {
    START("start", "Команда для запуска бота", "/start", Set.of(0)),
    HELP(
            "help",
            "Показывает все команды.\n/help [название команды]\nдля подробной информации",
            "/help\n/help [название команды]",
            Set.of(0, 1)),
    MENU("menu", "Открыть меню", "/menu - открыть меню", Set.of(0));

    private final String alias;
    private final String description;
    private final String extendedDescription;
    private final Set<Integer> validArgumentCounts;

    Command(String alias, String description, String extendedDescription, Set<Integer> validArgumentCounts) {
        this.alias = alias;
        this.description = description;
        this.extendedDescription = extendedDescription;
        this.validArgumentCounts = validArgumentCounts;
    }
}
