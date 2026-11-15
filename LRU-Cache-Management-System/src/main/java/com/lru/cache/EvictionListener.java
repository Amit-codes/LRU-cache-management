package com.lru.cache;

/** Callback fired whenever the cache removes an entry on its own (capacity overflow or shrinking). */
@FunctionalInterface
public interface EvictionListener<K, V> {
    void onEviction(K key, V value);
}
