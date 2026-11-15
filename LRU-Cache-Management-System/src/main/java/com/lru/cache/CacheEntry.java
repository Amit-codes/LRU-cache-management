package com.lru.cache;

/** Immutable key/value snapshot used to hand cache contents to the UI without exposing internal nodes. */
public record CacheEntry<K, V>(K key, V value) {
    @Override
    public String toString() {
        return key + "=" + value;
    }
}
