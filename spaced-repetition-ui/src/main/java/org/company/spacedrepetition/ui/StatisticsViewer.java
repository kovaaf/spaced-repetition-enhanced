package org.company.spacedrepetition.ui;

import org.company.spacedrepetition.ui.frame.MainFrame;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Swing application entry point for viewing spaced repetition statistics.
 * This class creates and displays the MainFrame with the complete UI layout.
 */
public class StatisticsViewer {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsViewer.class);
    
    /**
     * Constructs the application and creates the main frame.
     */
    public StatisticsViewer() {
        createAndShowGUI();
    }
    
    /**
     * Creates and displays the main application window.
     */
    private void createAndShowGUI() {
        // Create the main frame with complete UI layout
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
        
        LOG.info("Statistics Viewer started successfully");
        LOG.info("Data service URL: localhost:9091 (default)");
        LOG.info("Main frame layout: BorderLayout with three panels (filters, table, status)");
    }
    
    /**
     * Main entry point for the application.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Use SwingUtilities.invokeLater to ensure thread safety
        SwingUtilities.invokeLater(StatisticsViewer::new);
    }
}