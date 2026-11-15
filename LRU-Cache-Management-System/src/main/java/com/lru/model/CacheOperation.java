package com.lru.model;

import com.lru.cache.CacheEntry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Immutable record of one executed operation, used for the history log and for telling the UI
 * which key to highlight and which entries were evicted.
 */
public record CacheOperation(OperationType type,
                             String key,
                             String value,
                             boolean success,
                             String message,
                             List<CacheEntry<String, String>> evicted,
                             LocalTime timestamp) {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public CacheOperation {
        evicted = List.copyOf(evicted);
    }

    public boolean causedEviction() {
        return !evicted.isEmpty();
    }

    /** Short label such as {@code PUT(1, 100)} or {@code CLEAR()}. */
    public String signature() {
        return switch (type) {
            case PUT -> "PUT(" + key + ", " + value + ")";
            case GET, REMOVE, PEEK -> type + "(" + key + ")";
            case RESIZE -> "RESIZE(" + key + ")";
            case CLEAR -> "CLEAR()";
        };
    }

    /** One-line text for the history list. */
    public String describe() {
        return "[" + TIME.format(timestamp) + "] " + signature() + "  ->  " + message;
    }
}
