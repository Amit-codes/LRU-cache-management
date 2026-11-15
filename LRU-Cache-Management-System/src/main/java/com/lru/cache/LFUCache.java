package com.lru.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Least Frequently Used cache with O(1) operations (frequency buckets).
 * Ties between equally frequent keys are broken by recency, so the oldest key in the
 * lowest-frequency bucket is evicted. Used by the algorithm-comparison panel.
 */
public class LFUCache<K, V> extends AbstractCache<K, V> {

    private final Map<K, V> values = new HashMap<>();
    private final Map<K, Integer> frequencies = new HashMap<>();
    private final Map<Integer, LinkedHashSet<K>> buckets = new HashMap<>();
    private int minFrequency = 0;

    public LFUCache(int capacity) {
        super(capacity);
    }

    @Override
    public V put(K key, V value) {
        requireKey(key);
        requireValue(value);
        if (values.containsKey(key)) {
            V old = values.put(key, value);
            touch(key);
            return old;
        }
        while (values.size() >= capacity) {
            evictOne();
        }
        values.put(key, value);
        frequencies.put(key, 1);
        buckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        minFrequency = 1;
        return null;
    }

    @Override
    public V get(K key) {
        requireKey(key);
        if (!values.containsKey(key)) {
            return null;
        }
        touch(key);
        return values.get(key);
    }

    @Override
    public V peek(K key) {
        requireKey(key);
        return values.get(key);
    }

    @Override
    public V remove(K key) {
        requireKey(key);
        if (!values.containsKey(key)) {
            return null;
        }
        detachFromBucket(key, frequencies.remove(key));
        return values.remove(key);
    }

    @Override
    public void clear() {
        values.clear();
        frequencies.clear();
        buckets.clear();
        minFrequency = 0;
    }

    @Override
    public void resize(int newCapacity) {
        validateCapacity(newCapacity);
        this.capacity = newCapacity;
        while (values.size() > capacity) {
            evictOne();
        }
    }

    @Override
    public boolean containsKey(K key) {
        requireKey(key);
        return values.containsKey(key);
    }

    @Override
    public int size() {
        return values.size();
    }

    /** Highest frequency first; inside a bucket, most recently used first. Last element is the next victim. */
    @Override
    public List<CacheEntry<K, V>> entries() {
        List<CacheEntry<K, V>> result = new ArrayList<>(values.size());
        for (Integer freq : new TreeSet<>(buckets.keySet()).descendingSet()) {
            List<K> keys = new ArrayList<>(buckets.get(freq));
            Collections.reverse(keys);
            for (K k : keys) {
                result.add(new CacheEntry<>(k, values.get(k)));
            }
        }
        return result;
    }

    @Override
    public String policyName() {
        return "LFU";
    }

    /** Moves a key from bucket f to bucket f+1. */
    private void touch(K key) {
        int freq = frequencies.get(key);
        detachFromBucket(key, freq);
        frequencies.put(key, freq + 1);
        buckets.computeIfAbsent(freq + 1, f -> new LinkedHashSet<>()).add(key);
    }

    private void detachFromBucket(K key, int freq) {
        LinkedHashSet<K> bucket = buckets.get(freq);
        bucket.remove(key);
        if (bucket.isEmpty()) {
            buckets.remove(freq);
        }
    }

    private void evictOne() {
        if (!buckets.containsKey(minFrequency)) {          // stale after remove()/clear(); recompute lazily
            minFrequency = Collections.min(buckets.keySet());
        }
        LinkedHashSet<K> bucket = buckets.get(minFrequency);
        K victim = bucket.iterator().next();
        bucket.remove(victim);
        if (bucket.isEmpty()) {
            buckets.remove(minFrequency);
        }
        frequencies.remove(victim);
        V value = values.remove(victim);
        notifyEviction(victim, value);
    }
}
