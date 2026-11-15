package com.lru.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First-In-First-Out baseline: evicts the oldest inserted entry, ignoring reads.
 * Used by the algorithm-comparison panel.
 */
public class FIFOCache<K, V> extends AbstractCache<K, V> {

    private final LinkedHashMap<K, V> store = new LinkedHashMap<>(); // insertion-ordered

    public FIFOCache(int capacity) {
        super(capacity);
    }

    @Override
    public V put(K key, V value) {
        requireKey(key);
        requireValue(value);
        if (store.containsKey(key)) {
            return store.put(key, value);      // updating does not change insertion order
        }
        store.put(key, value);
        while (store.size() > capacity) {
            evictOldest();
        }
        return null;
    }

    @Override
    public V get(K key) {
        requireKey(key);
        return store.get(key);                 // reads never reorder in FIFO
    }

    @Override
    public V peek(K key) {
        return get(key);
    }

    @Override
    public V remove(K key) {
        requireKey(key);
        return store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public void resize(int newCapacity) {
        validateCapacity(newCapacity);
        this.capacity = newCapacity;
        while (store.size() > capacity) {
            evictOldest();
        }
    }

    @Override
    public boolean containsKey(K key) {
        requireKey(key);
        return store.containsKey(key);
    }

    @Override
    public int size() {
        return store.size();
    }

    /** Newest first, oldest (next to be evicted) last. */
    @Override
    public List<CacheEntry<K, V>> entries() {
        List<CacheEntry<K, V>> result = new ArrayList<>(store.size());
        for (Map.Entry<K, V> e : store.entrySet()) {
            result.add(new CacheEntry<>(e.getKey(), e.getValue()));
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public String policyName() {
        return "FIFO";
    }

    private void evictOldest() {
        Iterator<Map.Entry<K, V>> it = store.entrySet().iterator();
        Map.Entry<K, V> oldest = it.next();
        K key = oldest.getKey();
        V value = oldest.getValue();
        it.remove();
        notifyEviction(key, value);
    }
}
