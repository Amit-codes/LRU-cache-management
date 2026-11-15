package com.lru.model;

/**
 * A request to run an operation, before it has been executed. Produced by the script parser
 * and consumed by {@link com.lru.cache.CacheManager#execute(CacheCommand)}.
 */
public record CacheCommand(OperationType type, String key, String value, int capacity) {

    public static CacheCommand put(String key, String value) {
        return new CacheCommand(OperationType.PUT, key, value, 0);
    }

    public static CacheCommand get(String key) {
        return new CacheCommand(OperationType.GET, key, null, 0);
    }

    public static CacheCommand remove(String key) {
        return new CacheCommand(OperationType.REMOVE, key, null, 0);
    }

    public static CacheCommand peek(String key) {
        return new CacheCommand(OperationType.PEEK, key, null, 0);
    }

    public static CacheCommand clear() {
        return new CacheCommand(OperationType.CLEAR, null, null, 0);
    }

    public static CacheCommand resize(int capacity) {
        return new CacheCommand(OperationType.RESIZE, null, null, capacity);
    }
}
