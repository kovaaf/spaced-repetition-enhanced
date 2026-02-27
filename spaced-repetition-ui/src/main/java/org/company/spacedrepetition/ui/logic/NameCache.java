package org.company.spacedrepetition.ui.logic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple thread-safe cache with time-to-live (TTL) for name resolution.
 * Caches mappings from IDs to names (user, deck, card) to avoid repeated lookups.
 * Entries expire after a configurable TTL (default: 5 minutes).
 */
public class NameCache {
    
    private static final long DEFAULT_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes
    /**
     * Shared singleton instance.
     */
    private static final NameCache INSTANCE = new NameCache();

    /**
     * Returns the shared singleton instance.
     * @return the singleton NameCache instance
     */
    public static NameCache getInstance() {
        return INSTANCE;
    }
    
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;
    
    /**
     * Creates a NameCache with the default TTL (5 minutes).
     */
    public NameCache() {
        this(DEFAULT_TTL_MILLIS);
    }
    
    /**
     * Creates a NameCache with custom TTL.
     * @param ttlMillis time-to-live in milliseconds
     */
    public NameCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }
    
    /**
     * Stores a name mapping for the given ID.
     * If the name is null or empty, the mapping is not stored.
     * @param id the ID (user ID, deck ID, or card ID)
     * @param name the corresponding name (user name, deck name, or card title)
     */
    public void put(String id, String name) {
        if (id == null || name == null || name.trim().isEmpty()) {
            return;
        }
        long expiryTime = System.currentTimeMillis() + ttlMillis;
        cache.put(id, new CacheEntry(name, expiryTime));
    }
    
    /**
     * Retrieves the cached name for the given ID.
     * Returns null if the ID is not cached or the entry has expired.
     * @param id the ID to look up
     * @return the cached name, or null if not found/expired
     */
    public String get(String id) {
        if (id == null) {
            return null;
        }
        CacheEntry entry = cache.get(id);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(id, entry);
            return null;
        }
        return entry.value;
    }
    
    /**
     * Clears all cached entries.
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Returns the number of cached entries (including expired ones not yet evicted).
     * @return cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Removes expired entries from the cache.
     * This operation iterates over all entries; use sparingly.
     * @return number of entries removed
     */
    public int cleanupExpired() {
        int removed = 0;
        for (ConcurrentMap.Entry<String, CacheEntry> e : cache.entrySet()) {
            if (e.getValue().isExpired()) {
                if (cache.remove(e.getKey(), e.getValue())) {
                    removed++;
                }
            }
        }
        return removed;
    }
    
    // Inner class representing a cache entry with expiry
    private static class CacheEntry {
        final String value;
        final long expiryTime;
        
        CacheEntry(String value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}