package com.lru.cache;

import java.util.List;

/**
 * Abstraction shared by every eviction policy (LRU, FIFO, LFU).
 * Code that depends on this interface can swap policies freely (polymorphism).
 */
public interface Cache<K, V> {

    /** Inserts or updates an entry. Returns the previous value, or {@code null} if the key was new. */
    V put(K key, V value);

    /** Returns the value (or {@code null}) and lets the policy record the access. */
    V get(K key);

    /** Returns the value (or {@code null}) WITHOUT affecting eviction order. */
    V peek(K key);

    /** Removes an entry. Returns the removed value, or {@code null} if absent. */
    V remove(K key);

    /** Removes every entry. */
    void clear();

    /** Changes capacity and evicts entries when the new capacity is smaller than the current size. */
    void resize(int newCapacity);

    boolean containsKey(K key);

    int size();

    int capacity();

    /** Snapshot of the entries, ordered from "safest to keep" to "next to evict". */
    List<CacheEntry<K, V>> entries();

    void setEvictionListener(EvictionListener<K, V> listener);

    /** Human readable policy name, e.g. "LRU". */
    String policyName();
}
