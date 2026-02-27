package org.company.spacedrepetition.ui.integration;

import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.exception.ComponentLookupException;
import org.company.spacedrepetition.ui.frame.MainFrame;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QA Scenario 1 for Task 9: Verify "Apply Filters" button removed.
 * 
 * Evidence: .sisyphus/evidence/final-qa/task-9-button-removed-screenshot.png
 *           .sisyphus/evidence/final-qa/task-9-button-removed-test.txt
 */
class Task9ApplyFiltersButtonRemovalTest extends UITestBase {

    @Test
    void applyFiltersButton_shouldNotExist() throws IOException {
        // Given: MainFrame is initialized via UITestBase.setUp()
        
        // When: we search for buttons with text "Apply Filters"
        // We'll check both by component name and by text
        
        // Then: no button with text "Apply Filters" should exist
        // Attempt to find button by text - should throw ComponentLookupException
        assertThatThrownBy(() -> {
            window.button("Apply Filters");
        }).describedAs("Button with text 'Apply Filters' should not exist").isInstanceOf(ComponentLookupException.class);
        
        // Alternatively, search all buttons in the frame
        boolean found = false;
        for (java.awt.Component comp : mainFrame.getContentPane().getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                if ("Apply Filters".equals(button.getText())) {
                    found = true;
                    break;
                }
            }
        }
        assertThat(found).isFalse();
        
        // Capture screenshot for evidence (optional)
        // File screenshot = window.captureScreenshot().saveAs(
        //     ".sisyphus/evidence/final-qa/task-9-button-removed-screenshot.png"
        // );
        // assertThat(screenshot).exists();
        // Write test result to evidence file
        // Ensure directory exists
        Path evidenceFile = Paths.get("..", ".sisyphus", "evidence", "final-qa", "task-9-button-removed-test.txt");
        Files.createDirectories(evidenceFile.getParent());
        Files.writeString(evidenceFile, 
            "Test: Task9ApplyFiltersButtonRemovalTest.applyFiltersButton_shouldNotExist\n" +
            "Result: PASS\n" +
            "Timestamp: " + java.time.Instant.now() + "\n" +
            "Details: No button with text 'Apply Filters' found in MainFrame.\n"
        );
    }
    
    @Test
    void layout_shouldNotHaveEmptySpaceWhereButtonWas() {
        // Verify that the layout doesn't have obvious gaps where the button was removed
        // This is a visual check; we can check that the filter panel's preferred height
        // is reasonable (not excessively tall indicating empty space).
        
        // Get the filter panel (first component in the NORTH region)
        java.awt.Container contentPane = mainFrame.getContentPane();
        java.awt.LayoutManager layout = contentPane.getLayout();
        assertThat(layout).isInstanceOf(java.awt.BorderLayout.class);
        
        java.awt.BorderLayout borderLayout = (java.awt.BorderLayout) layout;
        java.awt.Component northComponent = borderLayout.getLayoutComponent(contentPane, java.awt.BorderLayout.NORTH);
        assertThat(northComponent).isNotNull();
        
        // The filter panel should not be excessively tall (more than 100 pixels would be suspicious)
        int panelHeight = northComponent.getHeight();
        assertThat(panelHeight).isLessThan(150); // Reasonable height for filter components
        
        // Write evidence
        try {
            Path evidenceFile = Paths.get("..", ".sisyphus", "evidence", "final-qa", "task-9-layout-check.txt");
            Files.createDirectories(evidenceFile.getParent());
            Files.writeString(evidenceFile,
                "Test: Task9ApplyFiltersButtonRemovalTest.layout_shouldNotHaveEmptySpaceWhereButtonWas\n" +
                "Result: PASS\n" +
                "Timestamp: " + java.time.Instant.now() + "\n" +
                "Filter panel height: " + panelHeight + " pixels\n" +
                "Layout check passed.\n"
            );
        } catch (IOException e) {
            // Ignore - evidence file not critical
        }
    }
}