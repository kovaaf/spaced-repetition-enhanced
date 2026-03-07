package org.company.spacedrepetitionbot.handler.handlers.callback.strategy;

import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackStrategy {
    void executeCallbackQuery(CallbackQuery callbackQuery);

    Callback getPrefix();
}
