package org.company.spacedrepetitionbot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class UpdateDispatcher {
    private final Map<String, NonCommandHandler> nonCommandHandlers;

    public UpdateDispatcher(List<NonCommandHandler> nonCommandHandlers) {
        log.debug(
                "Available handlers: {}",
                nonCommandHandlers.stream().map(h -> h.getClass().getSimpleName()).collect(Collectors.toList()));

        this.nonCommandHandlers = nonCommandHandlers.stream()
                .collect(toMap(NonCommandHandler::getHandlerName, Function.identity()));
    }

    public void handle(Update update) {
        //        nonCommandHandlers.forEach((k,v) ->
        //                System.out.printf("Идентификатор: %s, описание: %s%n", k, v.toString()));

        if (update.hasCallbackQuery()) {
            nonCommandHandlers.get("callBackHandler").handle(update);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            nonCommandHandlers.get("textHandler").handle(update);
        } else if (update.hasMessage()) {
            nonCommandHandlers.get("nonTextHandler").handle(update);
        } else if (update.hasEditedMessage() && update.getEditedMessage().hasText()) {
            nonCommandHandlers.get("editedMessageHandler").handle(update);
        }
    }
}
