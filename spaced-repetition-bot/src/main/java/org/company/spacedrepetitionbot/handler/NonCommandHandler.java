package org.company.spacedrepetitionbot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface NonCommandHandler {
    void handle(Update update);

    String getHandlerName();
}
