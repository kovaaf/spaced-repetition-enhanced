package org.company.data;

import lombok.extern.slf4j.Slf4j;
import org.company.data.config.AppContext;

/**
 * Main entry point for the data service.
 */
@Slf4j
public class DataServiceRunner {
    public static void main(String[] args) {
        AppContext.run();
    }
}