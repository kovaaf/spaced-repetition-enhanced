package org.company.spacedrepetition.ui.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NameCache}.
 */
class NameCacheTest {

    private NameCache cache;

    @BeforeEach
    void setUp() {
        // Clear the singleton instance before each test
        NameCache.getInstance().clear();
        cache = NameCache.getInstance();
    }

    @Test
    void getInstance_shouldReturnSameInstance() {
        // When
        NameCache instance1 = NameCache.getInstance();
        NameCache instance2 = NameCache.getInstance();

        // Then
        assertSame(instance1, instance2, "getInstance should return the same singleton instance");
    }

    @Test
    void put_shouldStoreValidName() {
        // Given
        String id = "user123";
        String name = "John Doe";

        // When
        cache.put(id, name);

        // Then
        assertEquals(name, cache.get(id), "Cache should return stored name");
    }

    @Test
    void put_shouldIgnoreNullId() {
        // Given
        String name = "John Doe";

        // When
        cache.put(null, name);

        // Then
        assertEquals(0, cache.size(), "Cache should be empty when null ID is provided");
    }

    @Test
    void put_shouldIgnoreNullName() {
        // Given
        String id = "user123";

        // When
        cache.put(id, null);

        // Then
        assertNull(cache.get(id), "Cache should not store null name");
        assertEquals(0, cache.size(), "Cache should be empty when null name is provided");
    }

    @Test
    void put_shouldIgnoreEmptyName() {
        // Given
        String id = "user123";
        String emptyName = "   ";

        // When
        cache.put(id, emptyName);

        // Then
        assertNull(cache.get(id), "Cache should not store empty or whitespace-only name");
        assertEquals(0, cache.size(), "Cache should be empty when empty name is provided");
    }

    @Test
    void get_shouldReturnNullForUnknownId() {
        // Given
        String unknownId = "unknown";

        // When
        String result = cache.get(unknownId);

        // Then
        assertNull(result, "Cache should return null for unknown ID");
    }

    @Test
    void get_shouldReturnNullForNullId() {
        // When
        String result = cache.get(null);

        // Then
        assertNull(result, "Cache should return null for null ID");
    }

    @Test
    void clear_shouldRemoveAllEntries() {
        // Given
        cache.put("user1", "Alice");
        cache.put("user2", "Bob");
        assertEquals(2, cache.size(), "Cache should have 2 entries before clear");

        // When
        cache.clear();

        // Then
        assertEquals(0, cache.size(), "Cache should be empty after clear");
        assertNull(cache.get("user1"), "Cache should not return cleared entry");
        assertNull(cache.get("user2"), "Cache should not return cleared entry");
    }

    @Test
    void size_shouldReturnNumberOfEntries() {
        // Given & When
        cache.put("user1", "Alice");
        cache.put("user2", "Bob");
        cache.put("deck1", "Java Basics");
        cache.put("card1", "What is Java?");

        // Then
        assertEquals(4, cache.size(), "Cache size should match number of stored entries");
    }

    @Test
    void cleanupExpired_shouldRemoveExpiredEntries() throws InterruptedException {
        // Given: Create a cache with very short TTL (100ms)
        NameCache shortTtlCache = new NameCache(100);
        shortTtlCache.put("user1", "Alice");
        shortTtlCache.put("user2", "Bob");
        
        // Wait for entries to expire
        Thread.sleep(150);
        
        // When
        int removed = shortTtlCache.cleanupExpired();
        
        // Then
        assertEquals(2, removed, "Should remove both expired entries");
        assertEquals(0, shortTtlCache.size(), "Cache should be empty after cleanup");
    }

    @Test
    void cleanupExpired_shouldNotRemoveNonExpiredEntries() {
        // Given: Create a cache with long TTL (5 seconds)
        NameCache longTtlCache = new NameCache(5000);
        longTtlCache.put("user1", "Alice");
        longTtlCache.put("user2", "Bob");
        
        // When
        int removed = longTtlCache.cleanupExpired();
        
        // Then
        assertEquals(0, removed, "Should not remove non-expired entries");
        assertEquals(2, longTtlCache.size(), "Cache should still have 2 entries");
        assertEquals("Alice", longTtlCache.get("user1"), "Non-expired entry should still be accessible");
        assertEquals("Bob", longTtlCache.get("user2"), "Non-expired entry should still be accessible");
    }

    @Test
    void get_shouldReturnNullForExpiredEntry() throws InterruptedException {
        // Given: Create a cache with very short TTL (100ms)
        NameCache shortTtlCache = new NameCache(100);
        shortTtlCache.put("user1", "Alice");
        
        // Wait for entry to expire
        Thread.sleep(150);
        
        // When
        String result = shortTtlCache.get("user1");
        
        // Then
        assertNull(result, "Cache should return null for expired entry");
    }

    @Test
    void constructor_withCustomTtl_shouldUseSpecifiedTtl() throws InterruptedException {
        // Given: Create a cache with custom TTL (200ms)
        NameCache customTtlCache = new NameCache(200);
        customTtlCache.put("user1", "Alice");
        
        // Wait less than TTL
        Thread.sleep(100);
        
        // Then: Entry should still be accessible
        assertEquals("Alice", customTtlCache.get("user1"), "Entry should be accessible before TTL expires");
        
        // Wait for TTL to expire
        Thread.sleep(150);
        
        // Then: Entry should be expired
        assertNull(customTtlCache.get("user1"), "Entry should be expired after TTL");
    }

    @Test
    void put_shouldOverwriteExistingEntry() {
        // Given
        String id = "user123";
        cache.put(id, "Old Name");
        assertEquals("Old Name", cache.get(id), "Cache should have old name");
        
        // When
        cache.put(id, "New Name");
        
        // Then
        assertEquals("New Name", cache.get(id), "Cache should return new name after overwrite");
        assertEquals(1, cache.size(), "Cache should still have only one entry");
    }

    @Test
    void multipleTypesOfIds_shouldWorkCorrectly() {
        // Given & When
        cache.put("123", "User 123");  // User ID
        cache.put("456", "Deck 456");  // Deck ID  
        cache.put("789", "Card 789");  // Card ID
        
        // Then
        assertEquals("User 123", cache.get("123"), "Should retrieve user name");
        assertEquals("Deck 456", cache.get("456"), "Should retrieve deck name");
        assertEquals("Card 789", cache.get("789"), "Should retrieve card title");
        assertEquals(3, cache.size(), "Cache should have 3 entries");
    }
}