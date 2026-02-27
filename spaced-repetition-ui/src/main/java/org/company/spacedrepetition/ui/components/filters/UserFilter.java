package org.company.spacedrepetition.ui.components.filters;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.company.spacedrepetition.ui.logic.StatisticsDataFetcher;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
/**
 * User filter component for selecting users to view statistics for.
 * Loads real users from the analytics service (user_name column in bot.user_info table).
 * Provides a dropdown (JComboBox) with "All Users" option followed by real user nicknames.
 * If user loading fails, only "All Users" is shown.
 * 
 * Follows TRD Appendix E specifications for filter components.
 * Wrapped in a JPanel to maintain consistent styling with other filters.
 */
public class UserFilter extends JPanel {

    private static final Logger LOG = LoggerFactory.getLogger(UserFilter.class);
    
    static class UserItem {
        private final long id;
        private final String name;
        
        UserItem(long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public long getId() { return id; }

        public String getName() { return name; }
        
        @Override
        public String toString() {
            return name != null && !name.isEmpty() ? name : "User " + id;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UserItem) {
                return ((UserItem) obj).id == this.id;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }
    }

    private JComboBox<Object> userComboBox;
    private DefaultComboBoxModel<Object> userComboBoxModel;
    
    /**
     * Constructs a new UserFilter with default styling and placeholder data.
     * "All Users" is selected by default.
     */
    public UserFilter() {
        initializeComponents();
        setupLayout();
        setupStyling();
        setupListeners();
    }
    
    /**
     * Initializes the combo box with placeholder user items.
     */
    private void initializeComponents() {
        userComboBoxModel = new DefaultComboBoxModel<>();
        userComboBoxModel.addElement("All Users");
        userComboBox = new JComboBox<>(userComboBoxModel);
    }
    
    /**
     * Sets up the layout for the filter.
     * Uses BorderLayout to fill available space.
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        add(userComboBox, BorderLayout.CENTER);
    }
    
    /**
     * Applies consistent styling to all components.
     * Follows the existing UI's Dialog font and color scheme.
     */
    private void setupStyling() {
        Font comboFont = new Font("Dialog", Font.PLAIN, 12);
        userComboBox.setFont(comboFont);
        
        // Add visual border similar to placeholder components
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }
    
    /**
     * Sets up action listeners for the combo box.
     * Prints selection changes to console for verification.
     */
    private void setupListeners() {
        userComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedUserId = getSelectedUser();
                String logMessage = selectedUserId.isEmpty() ? "All Users" : "User ID: " + selectedUserId;
                LOG.info("User selected: " + logMessage);
            }
        });
    }
    
    /**
     * Gets the currently selected user.
     * @return the selected user as a string
     */
    public String getSelectedUser() {
        Object selected = userComboBox.getSelectedItem();
        if (selected instanceof String && "All Users".equals(selected)) {
            return "";
        } else if (selected instanceof UserItem) {
            return String.valueOf(((UserItem) selected).getId());
        } else {
            // fallback: try to parse as string
            return selected != null ? selected.toString() : "";
        }
    }
    
    /**
     * Sets the selected user by item text.
     * @param userText the text of the user to select
     * @return true if the user was found and selected, false otherwise
     */
    public boolean setSelectedUser(String userText) {
        // Handle empty or null text
        if (userText == null || userText.isEmpty()) {
            // Select "All Users"
            userComboBox.setSelectedIndex(0);
            return true;
        }
        
        // Iterate through items and compare display text
        for (int i = 0; i < userComboBox.getItemCount(); i++) {
            Object item = userComboBox.getItemAt(i);
            if (item != null && item.toString().equals(userText)) {
                userComboBox.setSelectedIndex(i);
                return true;
            }
        }
        // Not found
        return false;
    }
    
    /**
     * Gets the underlying JComboBox for direct manipulation if needed.
     * @return the JComboBox instance
     */
    public JComboBox<Object> getComboBox() {
        return userComboBox;
    }
    /**
     * Loads real users from the analytics service and populates the combo box.
     * Uses SwingWorker to avoid blocking the UI thread.
     * @param dataFetcher the statistics data fetcher to use for loading users
     */

    public void loadUsers(StatisticsDataFetcher dataFetcher) {
        dataFetcher.fetchUsers(
                response -> {
                // On success: update combo box model on EDT
                javax.swing.SwingUtilities.invokeLater(() -> {
                    // Clear existing items except "All Users"
                    userComboBoxModel.removeAllElements();
                    userComboBoxModel.addElement("All Users");
                    
                    // Add each user as a UserItem
                    for (AnalyticsProto.User user : response.getUsersList()) {
                        long userId = user.getId();
                        String userName = user.getName();
                        userComboBoxModel.addElement(new UserItem(userId, userName));
                    }
                    
                    LOG.info("Loaded {} users into filter", response.getUsersCount());
                });
            },
                exception -> {
                // On error: log error, keep placeholder items
                LOG.error("Failed to load users, keeping placeholder items", exception);
            }
        );
    }
}