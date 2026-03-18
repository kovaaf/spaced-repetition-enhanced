package org.company.ui;

import org.company.ui.infrastructure.config.AppContext;

/**
 * Application entry point.
 */
public class GUIRunner {
    public static void main(String[] args) {
        AppContext.createAndShowMainFrame();
    }
}