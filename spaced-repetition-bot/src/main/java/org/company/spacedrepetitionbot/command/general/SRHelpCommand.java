package org.company.spacedrepetitionbot.command.general;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.company.spacedrepetitionbot.command.constant.Command.HELP;
import static org.telegram.telegrambots.extensions.bots.commandbot.commands.helpCommand.HelpCommand.getHelpText;
import static org.telegram.telegrambots.extensions.bots.commandbot.commands.helpCommand.HelpCommand.getManText;

@Slf4j
@Component
public class SRHelpCommand extends SpacedRepetitionCommand {
    private final ICommandRegistry registry;

    @Lazy
    public SRHelpCommand(ICommandRegistry iCommandRegistry) {
        super(HELP.getAlias(), HELP.getDescription(), HELP.getExtendedDescription(), HELP.getValidArgumentCounts());
        this.registry = iCommandRegistry;
    }

    // TODO починить
    @Override
    protected void performAction(TelegramClient telegramClient, String[] arguments) {
        if (arguments.length > 0) {
            IBotCommand command = registry.getRegisteredCommand(arguments[0]);
            String result = getManText(command);
            sendMessage(telegramClient, result);
        } else {
            String result = getHelpText(registry);
            sendMessage(telegramClient, result);
        }
    }
}
