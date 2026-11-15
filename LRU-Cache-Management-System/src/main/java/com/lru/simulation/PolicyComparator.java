package com.lru.simulation;

import com.lru.cache.Cache;
import com.lru.cache.FIFOCache;
import com.lru.cache.LFUCache;
import com.lru.cache.LRUCache;
import com.lru.exception.CacheException;
import com.lru.model.CacheStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Replays one request sequence against LRU, FIFO and LFU caches of equal capacity.
 * Each request is a GET; on a miss the key is loaded into the cache (classic cache-aside pattern).
 */
public final class PolicyComparator {

    /** Outcome for one policy. */
    public record Result(String policy, long hits, long misses, long evictions, double hitRatio) {
    }

    private PolicyComparator() {
    }

    public static List<String> parseRequests(String text) {
        if (text == null || text.isBlank()) {
            throw new CacheException("Enter at least one key, e.g. 1 2 3 1 4");
        }
        List<String> requests = new ArrayList<>();
        for (String token : text.trim().split("[,\\s]+")) {
            if (!token.isEmpty()) {
                requests.add(token);
            }
        }
        return requests;
    }

    public static List<Result> compare(List<String> requests, int capacity) {
        List<Cache<String, String>> caches = List.of(
                new LRUCache<>(capacity), new FIFOCache<>(capacity), new LFUCache<>(capacity));
        List<Result> results = new ArrayList<>();
        for (Cache<String, String> cache : caches) {
            results.add(replay(cache, requests));
        }
        return results;
    }

    private static Result replay(Cache<String, String> cache, List<String> requests) {
        CacheStatistics stats = new CacheStatistics();
        cache.setEvictionListener((k, v) -> stats.recordEviction());
        for (String key : requests) {
            if (cache.get(key) != null) {
                stats.recordHit();
            } else {
                stats.recordMiss();
                cache.put(key, "value-" + key);
            }
        }
        return new Result(cache.policyName(), stats.getHits(), stats.getMisses(),
                stats.getEvictions(), stats.getHitRatio());
    }
}
