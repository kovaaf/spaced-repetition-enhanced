package org.company;

import org.company.config.AppContext;

/**
 * Application entry point.
 * Simply delegates to {@link AppContext#createAndShowMainFrame()}.
 */
public class Main {
    public static void main(String[] args) {
        AppContext.createAndShowMainFrame();
    }
}