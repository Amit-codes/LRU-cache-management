package com.lru.cache;

import com.lru.exception.CacheException;
import com.lru.exception.InvalidKeyException;
import com.lru.exception.InvalidValueException;
import com.lru.model.CacheCommand;
import com.lru.model.CacheOperation;
import com.lru.model.CacheStatistics;
import com.lru.model.OperationType;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Facade between the UI and a {@link Cache}. It validates user input, records statistics and
 * history, captures automatic evictions and tells listeners after every operation.
 * The UI never touches the cache directly.
 */
public class CacheManager {

    private static final int MAX_HISTORY = 200;

    private final Cache<String, String> cache;
    private final CacheStatistics statistics = new CacheStatistics();
    private final Deque<CacheOperation> history = new ArrayDeque<>();
    private final List<Consumer<CacheOperation>> listeners = new ArrayList<>();
    private final List<CacheEntry<String, String>> pendingEvictions = new ArrayList<>();

    public CacheManager(int capacity) {
        this(new LRUCache<>(capacity));
    }

    public CacheManager(Cache<String, String> cache) {
        this.cache = cache;
        this.cache.setEvictionListener((k, v) -> {
            pendingEvictions.add(new CacheEntry<>(k, v));
            statistics.recordEviction();
        });
    }

    // ---------------------------------------------------------------- operations

    public CacheOperation put(String key, String value) {
        String k = normalizeKey(key);
        String v = normalizeValue(value);
        boolean existed = cache.containsKey(k);
        String old = cache.put(k, v);
        statistics.recordPut();
        String message = existed
                ? "Updated key " + k + " (" + old + " -> " + v + "), now most recently used"
                : "Inserted key " + k + " as most recently used";
        return finish(OperationType.PUT, k, v, true, message);
    }

    public CacheOperation get(String key) {
        String k = normalizeKey(key);
        String value = cache.get(k);
        if (value == null) {
            statistics.recordMiss();
            return finish(OperationType.GET, k, null, false, "MISS - key not found");
        }
        statistics.recordHit();
        return finish(OperationType.GET, k, value, true, "HIT - value " + value + ", moved to most recently used");
    }

    public CacheOperation peek(String key) {
        String k = normalizeKey(key);
        String value = cache.peek(k);
        if (value == null) {
            return finish(OperationType.PEEK, k, null, false, "Key not found");
        }
        return finish(OperationType.PEEK, k, value, true, "Value " + value + " (usage order unchanged)");
    }

    public CacheOperation remove(String key) {
        String k = normalizeKey(key);
        String removed = cache.remove(k);
        if (removed == null) {
            return finish(OperationType.REMOVE, k, null, false, "Nothing to remove - key not found");
        }
        return finish(OperationType.REMOVE, k, removed, true, "Removed key " + k);
    }

    public CacheOperation clear() {
        int count = cache.size();
        cache.clear();
        return finish(OperationType.CLEAR, null, null, true, "Cleared " + count + " entr" + (count == 1 ? "y" : "ies"));
    }

    public CacheOperation resize(int newCapacity) {
        int old = cache.capacity();
        cache.resize(newCapacity);   // throws InvalidCapacityException for values < 1
        return finish(OperationType.RESIZE, String.valueOf(newCapacity), null, true,
                "Capacity " + old + " -> " + newCapacity);
    }

    /** Runs a parsed command; used by the simulation engine. */
    public CacheOperation execute(CacheCommand command) {
        return switch (command.type()) {
            case PUT -> put(command.key(), command.value());
            case GET -> get(command.key());
            case PEEK -> peek(command.key());
            case REMOVE -> remove(command.key());
            case CLEAR -> clear();
            case RESIZE -> resize(command.capacity());
        };
    }

    // ---------------------------------------------------------------- queries

    /** Entries ordered most recently used first, least recently used last. */
    public List<CacheEntry<String, String>> snapshot() {
        return cache.entries();
    }

    public int size() {
        return cache.size();
    }

    public int capacity() {
        return cache.capacity();
    }

    public String policyName() {
        return cache.policyName();
    }

    /** Fraction of capacity currently in use (0.0 - 1.0). */
    public double utilization() {
        return (double) cache.size() / cache.capacity();
    }

    public CacheStatistics statistics() {
        return statistics;
    }

    /** Newest operation first. */
    public List<CacheOperation> history() {
        List<CacheOperation> copy = new ArrayList<>(history);
        Collections.reverse(copy);
        return copy;
    }

    public void resetStatistics() {
        statistics.reset();
    }

    public void addListener(Consumer<CacheOperation> listener) {
        listeners.add(listener);
    }

    // ---------------------------------------------------------------- internals

    private CacheOperation finish(OperationType type, String key, String value, boolean success, String message) {
        List<CacheEntry<String, String>> evicted = new ArrayList<>(pendingEvictions);
        pendingEvictions.clear();
        if (!evicted.isEmpty()) {
            StringBuilder sb = new StringBuilder(message).append(" | EVICTED: ");
            for (int i = 0; i < evicted.size(); i++) {
                sb.append(i > 0 ? ", " : "").append(evicted.get(i).key());
            }
            message = sb.toString();
        }
        CacheOperation op = new CacheOperation(type, key, value, success, message, evicted, LocalTime.now());
        history.addLast(op);
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
        listeners.forEach(l -> l.accept(op));
        return op;
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key must not be empty");
        }
        return key.trim();
    }

    private static String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("Value must not be empty");
        }
        return value.trim();
    }

    /** Parses a capacity typed by the user. */
    public static int parseCapacity(String text) {
        try {
            int n = Integer.parseInt(text.trim());
            if (n <= 0) {
                throw new com.lru.exception.InvalidCapacityException("Capacity must be a positive integer, got: " + n);
            }
            return n;
        } catch (NumberFormatException | NullPointerException e) {
            throw new CacheException("Capacity must be a whole number, got: " + text);
        }
    }
}
