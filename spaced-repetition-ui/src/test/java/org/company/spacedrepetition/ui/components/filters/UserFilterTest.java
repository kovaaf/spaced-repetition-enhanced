package org.company.spacedrepetition.ui.components.filters;

import org.company.spacedrepetition.ui.logic.StatisticsDataFetcher;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserFilter}.
 */
@ExtendWith(MockitoExtension.class)
class UserFilterTest {

    @Mock
    private StatisticsDataFetcher mockDataFetcher;

    @Test
    void constructor_shouldInitializeWithAllUsersOption() {
        // When
        UserFilter filter = new UserFilter();

        // Then
        assertEquals(1, filter.getComboBox().getItemCount());
        assertEquals("All Users", filter.getComboBox().getItemAt(0));
        assertEquals("", filter.getSelectedUser()); // "All Users" returns empty string
    }

    @Test
    void getSelectedUser_withAllUsers_returnsEmptyString() {
        // Given
        UserFilter filter = new UserFilter();
        filter.getComboBox().setSelectedIndex(0); // "All Users"

        // When
        String selected = filter.getSelectedUser();

        // Then
        assertEquals("", selected);
    }

    @Test
    void setSelectedUser_withEmptyString_selectsAllUsers() {
        // Given
        UserFilter filter = new UserFilter();
        // Add some placeholder items for testing
        filter.getComboBox().addItem(new UserFilter.UserItem(1, "Test User"));
        filter.getComboBox().setSelectedIndex(1); // Select user item

        // When
        boolean result = filter.setSelectedUser("");

        // Then
        assertTrue(result);
        assertEquals("All Users", filter.getComboBox().getSelectedItem());
        assertEquals("", filter.getSelectedUser());
    }

    @Test
    void setSelectedUser_withExistingUserText_selectsUser() {
        // Given
        UserFilter filter = new UserFilter();
        // Add some placeholder items
        UserFilter.UserItem user1 = new UserFilter.UserItem(1, "User One");
        UserFilter.UserItem user2 = new UserFilter.UserItem(2, "User Two");
        filter.getComboBox().addItem(user1);
        filter.getComboBox().addItem(user2);

        // When
        boolean result = filter.setSelectedUser("User Two");

        // Then
        assertTrue(result);
        assertEquals(user2, filter.getComboBox().getSelectedItem());
        assertEquals("2", filter.getSelectedUser());
    }

    @Test
    void setSelectedUser_withNonExistentUserText_returnsFalse() {
        // Given
        UserFilter filter = new UserFilter();
        filter.getComboBox().addItem(new UserFilter.UserItem(1, "User One"));

        // When
        boolean result = filter.setSelectedUser("Non Existent");

        // Then
        assertFalse(result);
        // Selection should remain unchanged (default is "All Users")
        assertEquals("All Users", filter.getComboBox().getSelectedItem());
    }

    @Test
    void loadUsers_shouldCallFetchUsersAndPopulateComboBox() throws Exception {
        // Given
        UserFilter filter = new UserFilter();
        ArgumentCaptor<Consumer<AnalyticsProto.UsersResponse>> successCaptor = 
            ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<Exception>> errorCaptor = 
            ArgumentCaptor.forClass(Consumer.class);
        
        // Prepare mock response with real users
        AnalyticsProto.UsersResponse mockResponse = AnalyticsProto.UsersResponse.newBuilder()
            .addUsers(AnalyticsProto.User.newBuilder()
                .setId(123)
                .setName("John Doe")
                .build())
            .addUsers(AnalyticsProto.User.newBuilder()
                .setId(456)
                .setName("Jane Smith")
                .build())
            .addUsers(AnalyticsProto.User.newBuilder()
                .setId(789)
                .setName("") // empty name
                .build())
            .build();
        
        // When
        filter.loadUsers(mockDataFetcher);
        
        // Capture callbacks
        verify(mockDataFetcher).fetchUsers(successCaptor.capture(), errorCaptor.capture());
        
        // Simulate successful fetch by invoking success callback
        Consumer<AnalyticsProto.UsersResponse> successCallback = successCaptor.getValue();
        // Invoke callback directly (it will schedule EDT update)
            successCallback.accept(mockResponse);
        // Wait for EDT to process all pending events
        SwingUtilities.invokeAndWait(() -> {});
        
        // Then
        assertEquals(4, filter.getComboBox().getItemCount()); // "All Users" + 3 users
        assertEquals("All Users", filter.getComboBox().getItemAt(0));
        
        // Check first user
        Object item1 = filter.getComboBox().getItemAt(1);
        assertTrue(item1 instanceof UserFilter.UserItem);
        UserFilter.UserItem userItem1 = (UserFilter.UserItem) item1;
        assertEquals(123, userItem1.getId());
        assertEquals("John Doe", userItem1.getName());
        assertEquals("John Doe", userItem1.toString());
        
        // Check second user
        Object item2 = filter.getComboBox().getItemAt(2);
        assertTrue(item2 instanceof UserFilter.UserItem);
        UserFilter.UserItem userItem2 = (UserFilter.UserItem) item2;
        assertEquals(456, userItem2.getId());
        assertEquals("Jane Smith", userItem2.getName());
        assertEquals("Jane Smith", userItem2.toString());
        
        // Check third user with empty name
        Object item3 = filter.getComboBox().getItemAt(3);
        assertTrue(item3 instanceof UserFilter.UserItem);
        UserFilter.UserItem userItem3 = (UserFilter.UserItem) item3;
        assertEquals(789, userItem3.getId());
        assertEquals("", userItem3.getName());
        assertEquals("User 789", userItem3.toString()); // toString should fallback to "User {id}"
    }

    @Test
    void loadUsers_onError_shouldKeepAllUsersOnly() throws Exception {
        // Given
        UserFilter filter = new UserFilter();
        ArgumentCaptor<Consumer<AnalyticsProto.UsersResponse>> successCaptor = 
            ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<Exception>> errorCaptor = 
            ArgumentCaptor.forClass(Consumer.class);
        
        // When
        filter.loadUsers(mockDataFetcher);
        
        // Capture callbacks
        verify(mockDataFetcher).fetchUsers(successCaptor.capture(), errorCaptor.capture());
        
        // Simulate error by invoking error callback
        Consumer<Exception> errorCallback = errorCaptor.getValue();
        Exception testException = new RuntimeException("Network error");
        
        // Use latch to wait for SwingUtilities.invokeLater
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            errorCallback.accept(testException);
            latch.countDown();
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation timed out");
        
        // Then
        // Should still have only "All Users" (no users added)
        assertEquals(1, filter.getComboBox().getItemCount());
        assertEquals("All Users", filter.getComboBox().getItemAt(0));
    }


    @Test
    void loadUsers_withEmptyList_shouldKeepAllUsersOnly() throws Exception {
        // Given
        UserFilter filter = new UserFilter();
        ArgumentCaptor<Consumer<AnalyticsProto.UsersResponse>> successCaptor = 
            ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<Exception>> errorCaptor = 
            ArgumentCaptor.forClass(Consumer.class);
        
        // When
        filter.loadUsers(mockDataFetcher);
        
        // Capture callbacks
        verify(mockDataFetcher).fetchUsers(successCaptor.capture(), errorCaptor.capture());
        
        // Simulate successful fetch with empty list
        Consumer<AnalyticsProto.UsersResponse> successCallback = successCaptor.getValue();
        AnalyticsProto.UsersResponse emptyResponse = AnalyticsProto.UsersResponse.newBuilder().build();
        successCallback.accept(emptyResponse);
        // Wait for EDT to process all pending events
        SwingUtilities.invokeAndWait(() -> {});
        
        // Then
        assertEquals(1, filter.getComboBox().getItemCount());
        assertEquals("All Users", filter.getComboBox().getItemAt(0));
    }

    @Test
    void userItem_equalsAndHashCode() {
        UserFilter.UserItem item1 = new UserFilter.UserItem(123, "Test");
        UserFilter.UserItem item2 = new UserFilter.UserItem(123, "Different Name");
        UserFilter.UserItem item3 = new UserFilter.UserItem(456, "Test");
        
        // Equality based on ID only
        assertEquals(item1, item2);
        assertNotEquals(item1, item3);
        
        // Hash code consistency
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotEquals(item1.hashCode(), item3.hashCode());
        
        // Not equal to other types
        assertNotEquals(item1, "string");
        assertNotEquals(item1, null);
    }

    @Test
    void userItem_toString() {
        // With name
        UserFilter.UserItem item1 = new UserFilter.UserItem(123, "John Doe");
        assertEquals("John Doe", item1.toString());
        
        // With empty name
        UserFilter.UserItem item2 = new UserFilter.UserItem(456, "");
        assertEquals("User 456", item2.toString());
        
        // With null name (should not happen but test robustness)
        UserFilter.UserItem item3 = new UserFilter.UserItem(789, null);
        assertEquals("User 789", item3.toString());
    }
}