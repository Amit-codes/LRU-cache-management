package com.lru.cache;

import com.lru.exception.InvalidCapacityException;
import com.lru.exception.InvalidKeyException;
import com.lru.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LRUCacheTest {

    private static List<String> keys(Cache<String, String> cache) {
        return cache.entries().stream().map(CacheEntry::key).toList();
    }

    // ---------------------------------------------------------------- basic behaviour

    @Test
    @DisplayName("put then get returns the stored value")
    void insertAndRetrieve() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        assertNull(cache.put("a", "1"));
        assertEquals("1", cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    @DisplayName("get on a missing key returns null and changes nothing")
    void getMissingKey() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        cache.put("a", "1");
        assertNull(cache.get("zzz"));
        assertEquals(List.of("a"), keys(cache));
    }

    @Test
    @DisplayName("updating an existing key replaces the value, returns the old one and does not grow the cache")
    void updateExistingKey() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        cache.put("a", "1");
        assertEquals("1", cache.put("a", "2"));
        assertEquals("2", cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    @DisplayName("updating a key makes it the most recently used")
    void updateRefreshesRecency() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.put("a", "updated");
        assertEquals(List.of("a", "c", "b"), keys(cache));
    }

    // ---------------------------------------------------------------- eviction & ordering

    @Test
    @DisplayName("capacity-3 demo: PUT 1,2,3, GET 1, PUT 4 evicts key 2")
    void demoScenarioEvictsKeyTwo() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        List<String> evicted = new ArrayList<>();
        cache.setEvictionListener((k, v) -> evicted.add(k + "=" + v));

        cache.put("1", "100");
        cache.put("2", "200");
        cache.put("3", "300");
        cache.get("1");
        cache.put("4", "400");

        assertEquals(List.of("2=200"), evicted);
        assertFalse(cache.containsKey("2"));
        assertEquals(List.of("4", "1", "3"), keys(cache));   // MRU -> LRU
    }

    @Test
    @DisplayName("get moves an entry to the front")
    void accessOrderChanges() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        assertEquals(List.of("c", "b", "a"), keys(cache));
        cache.get("a");
        assertEquals(List.of("a", "c", "b"), keys(cache));
        cache.get("b");
        assertEquals(List.of("b", "a", "c"), keys(cache));
    }

    @Test
    @DisplayName("peek returns the value without changing usage order")
    void peekDoesNotChangeOrder() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        assertEquals("1", cache.peek("a"));
        assertEquals(List.of("c", "b", "a"), keys(cache));

        cache.put("d", "4");                      // "a" must still be the LRU and be evicted
        assertFalse(cache.containsKey("a"));
    }

    @Test
    @DisplayName("peek on a missing key returns null")
    void peekMissing() {
        assertNull(new LRUCache<String, String>(2).peek("nope"));
    }

    // ---------------------------------------------------------------- remove / clear

    @Test
    @DisplayName("remove deletes an entry and frees space")
    void removeExisting() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        cache.put("a", "1");
        cache.put("b", "2");
        assertEquals("1", cache.remove("a"));
        assertEquals(List.of("b"), keys(cache));
        cache.put("c", "3");                       // fits without evicting "b"
        assertEquals(List.of("c", "b"), keys(cache));
    }

    @Test
    @DisplayName("removing a middle node keeps the list consistent")
    void removeMiddleNode() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.remove("b");
        assertEquals(List.of("c", "a"), keys(cache));
    }

    @Test
    @DisplayName("removing a missing key returns null")
    void removeMissing() {
        assertNull(new LRUCache<String, String>(2).remove("ghost"));
    }

    @Test
    @DisplayName("manual removal and clear do not trigger the eviction listener")
    void removeAndClearAreNotEvictions() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        List<String> evicted = new ArrayList<>();
        cache.setEvictionListener((k, v) -> evicted.add(k));
        cache.put("a", "1");
        cache.put("b", "2");
        cache.remove("a");
        cache.clear();
        assertTrue(evicted.isEmpty());
    }

    @Test
    @DisplayName("clear empties the cache and it stays usable")
    void clearEmptiesCache() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.entries().isEmpty());
        cache.put("c", "3");
        assertEquals(List.of("c"), keys(cache));
    }

    // ---------------------------------------------------------------- resize

    @Test
    @DisplayName("shrinking evicts the least recently used entries first")
    void resizeShrinkEvicts() {
        LRUCache<String, String> cache = new LRUCache<>(4);
        List<String> evicted = new ArrayList<>();
        cache.setEvictionListener((k, v) -> evicted.add(k));
        for (String k : List.of("a", "b", "c", "d")) {
            cache.put(k, k);
        }
        cache.get("a");                            // order: a d c b
        cache.resize(2);
        assertEquals(2, cache.capacity());
        assertEquals(List.of("a", "d"), keys(cache));
        assertEquals(List.of("b", "c"), evicted);
    }

    @Test
    @DisplayName("growing keeps all entries and allows more inserts")
    void resizeGrow() {
        LRUCache<String, String> cache = new LRUCache<>(1);
        cache.put("a", "1");
        cache.resize(3);
        cache.put("b", "2");
        cache.put("c", "3");
        assertEquals(List.of("c", "b", "a"), keys(cache));
    }

    @Test
    @DisplayName("resize rejects non-positive capacity and leaves the cache untouched")
    void resizeInvalid() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        cache.put("a", "1");
        assertThrows(InvalidCapacityException.class, () -> cache.resize(0));
        assertThrows(InvalidCapacityException.class, () -> cache.resize(-5));
        assertEquals(2, cache.capacity());
        assertEquals(1, cache.size());
    }

    // ---------------------------------------------------------------- edge cases

    @Test
    @DisplayName("capacity 1 keeps only the latest entry")
    void capacityOne() {
        LRUCache<String, String> cache = new LRUCache<>(1);
        List<String> evicted = new ArrayList<>();
        cache.setEvictionListener((k, v) -> evicted.add(k));
        cache.put("a", "1");
        cache.put("b", "2");
        assertNull(cache.get("a"));
        assertEquals("2", cache.get("b"));
        assertEquals(List.of("a"), evicted);
        cache.put("b", "3");                        // update must not evict itself
        assertEquals(List.of("a"), evicted);
        assertEquals("3", cache.get("b"));
    }

    @Test
    @DisplayName("constructor rejects zero and negative capacity")
    void invalidCapacity() {
        assertThrows(InvalidCapacityException.class, () -> new LRUCache<String, String>(0));
        assertThrows(InvalidCapacityException.class, () -> new LRUCache<String, String>(-1));
    }

    @Test
    @DisplayName("null, empty and blank keys are rejected")
    void invalidKeys() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        assertThrows(InvalidKeyException.class, () -> cache.put(null, "v"));
        assertThrows(InvalidKeyException.class, () -> cache.put("", "v"));
        assertThrows(InvalidKeyException.class, () -> cache.put("   ", "v"));
        assertThrows(InvalidKeyException.class, () -> cache.get(null));
        assertThrows(InvalidKeyException.class, () -> cache.remove(""));
        assertThrows(InvalidKeyException.class, () -> cache.peek(null));
    }

    @Test
    @DisplayName("null values are rejected")
    void invalidValue() {
        assertThrows(InvalidValueException.class, () -> new LRUCache<String, String>(2).put("a", null));
    }

    @Test
    @DisplayName("operations after the cache was emptied by evictions still behave")
    void repeatedEvictionCycle() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(2);
        for (int i = 0; i < 100; i++) {
            cache.put(i, i * 10);
        }
        assertEquals(2, cache.size());
        assertEquals(List.of(99, 98), cache.entries().stream().map(CacheEntry::key).toList());
    }

    // ---------------------------------------------------------------- reference comparison

    @Test
    @DisplayName("20,000 random operations match java.util.LinkedHashMap in access-order mode")
    void matchesReferenceImplementation() {
        final int capacity = 7;
        LRUCache<Integer, Integer> cache = new LRUCache<>(capacity);
        Map<Integer, Integer> reference = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > capacity;
            }
        };
        Random random = new Random(42);
        for (int i = 0; i < 20_000; i++) {
            int key = random.nextInt(15);
            switch (random.nextInt(3)) {
                case 0 -> {
                    cache.put(key, i);
                    reference.put(key, i);
                }
                case 1 -> assertEquals(reference.get(key), cache.get(key));
                default -> assertEquals(reference.remove(key), cache.remove(key));
            }
            List<Integer> expected = new ArrayList<>(reference.keySet());   // LRU -> MRU
            java.util.Collections.reverse(expected);                        // MRU -> LRU
            assertEquals(expected, cache.entries().stream().map(CacheEntry::key).toList());
        }
    }
}
