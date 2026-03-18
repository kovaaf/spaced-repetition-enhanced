package org.company.spacedrepetitiondata;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.config.AppContext;

import java.io.IOException;

/**
 * Main entry point for the data service.
 */
@Slf4j
public class DataServiceRunner {
    public static void main(String[] args) throws IOException, InterruptedException {
        AppContext.run();
    }
}