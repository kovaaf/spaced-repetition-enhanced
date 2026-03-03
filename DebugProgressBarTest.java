import org.company.spacedrepetition.ui.frame.MainFrame;
import org.company.spacedrepetition.ui.client.AnalyticsServiceClient;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;

public class DebugProgressBarTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting debug test...");
        
        // Mock the static method
        try (MockedStatic<AnalyticsServiceClient> mockedStatic = Mockito.mockStatic(AnalyticsServiceClient.class)) {
            System.out.println("Mock set up");
            
            // Set up mock to delay and return a mock client
            mockedStatic.when(() -> AnalyticsServiceClient.createFromConfig())
                .thenAnswer(invocation -> {
                    System.out.println("[MOCK] createFromConfig called on thread " + Thread.currentThread().getId());
                    System.out.println("[MOCK] Sleeping for 2 seconds...");
                    Thread.sleep(2000);
                    System.out.println("[MOCK] Sleep complete, returning mock client");
                    return Mockito.mock(AnalyticsServiceClient.class);
                });
            
            // Create MainFrame on EDT
            System.out.println("Creating MainFrame on EDT...");
            SwingUtilities.invokeAndWait(() -> {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                
                // Check progress bar immediately
                JProgressBar progressBar = null;
                try {
                    java.lang.reflect.Field progressBarField = MainFrame.class.getDeclaredField("progressBar");
                    progressBarField.setAccessible(true);
                    progressBar = (JProgressBar) progressBarField.get(frame);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                if (progressBar != null) {
                    System.out.println("Progress bar visible: " + progressBar.isVisible());
                    System.out.println("Progress bar indeterminate: " + progressBar.isIndeterminate());
                }
            });
            
            System.out.println("MainFrame created. Waiting a bit...");
            Thread.sleep(3000);
            
            System.out.println("Test complete");
        }
    }
}