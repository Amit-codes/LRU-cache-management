package com.lru.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the FIFO and LFU baselines used by the comparison panel. */
class EvictionPolicyTest {

    @Test
    @DisplayName("FIFO evicts the oldest insertion even if it was just read")
    void fifoIgnoresReads() {
        FIFOCache<String, String> cache = new FIFOCache<>(2);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.get("a");
        cache.put("c", "3");
        assertFalse(cache.containsKey("a"));
        assertTrue(cache.containsKey("b"));
    }

    @Test
    @DisplayName("FIFO resize and remove")
    void fifoResizeAndRemove() {
        FIFOCache<String, String> cache = new FIFOCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.resize(1);
        assertEquals(List.of("c"), cache.entries().stream().map(CacheEntry::key).toList());
        assertEquals("3", cache.remove("c"));
        assertNull(cache.remove("c"));
    }

    @Test
    @DisplayName("LFU evicts the least frequently used key")
    void lfuEvictsLeastFrequent() {
        LFUCache<String, String> cache = new LFUCache<>(2);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.get("a");
        cache.get("a");
        cache.put("c", "3");
        assertTrue(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"));
    }

    @Test
    @DisplayName("LFU breaks frequency ties by evicting the older key")
    void lfuTieBreak() {
        LFUCache<String, String> cache = new LFUCache<>(2);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        assertFalse(cache.containsKey("a"));
    }

    @Test
    @DisplayName("LFU stays consistent after remove, clear and shrinking resize")
    void lfuRemoveClearResize() {
        LFUCache<String, String> cache = new LFUCache<>(4);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.get("c");
        cache.get("c");
        cache.get("b");
        cache.remove("a");                       // lowest-frequency bucket disappears
        cache.resize(1);                         // must keep the most frequent key "c"
        assertEquals(List.of("c"), cache.entries().stream().map(CacheEntry::key).toList());
        cache.clear();
        assertEquals(0, cache.size());
        cache.put("x", "1");
        assertEquals("1", cache.get("x"));
    }

    @Test
    @DisplayName("all policies share the same Cache contract (polymorphism)")
    void commonContract() {
        for (Cache<String, String> cache : List.<Cache<String, String>>of(new LRUCache<>(2), new FIFOCache<>(2), new LFUCache<>(2))) {
            cache.put("a", "1");
            cache.put("b", "2");
            cache.put("c", "3");
            assertEquals(2, cache.size(), cache.policyName());
            assertEquals("3", cache.peek("c"), cache.policyName());
        }
    }
}
