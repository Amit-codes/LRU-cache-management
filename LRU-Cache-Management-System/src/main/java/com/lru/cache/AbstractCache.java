package com.lru.cache;

import com.lru.exception.InvalidCapacityException;
import com.lru.exception.InvalidKeyException;
import com.lru.exception.InvalidValueException;

/**
 * Template for all policies: owns capacity, validation and the eviction listener,
 * so concrete caches only implement the ordering logic.
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    protected int capacity;
    private EvictionListener<K, V> evictionListener;

    protected AbstractCache(int capacity) {
        validateCapacity(capacity);
        this.capacity = capacity;
    }

    protected static void validateCapacity(int capacity) {
        if (capacity <= 0) {
            throw new InvalidCapacityException("Capacity must be a positive integer, got: " + capacity);
        }
    }

    protected void requireKey(K key) {
        if (key == null) {
            throw new InvalidKeyException("Key must not be null");
        }
        if (key instanceof CharSequence cs && cs.toString().isBlank()) {
            throw new InvalidKeyException("Key must not be empty or blank");
        }
    }

    protected void requireValue(V value) {
        if (value == null) {
            throw new InvalidValueException("Value must not be null");
        }
    }

    /** Subclasses call this after they have physically removed an entry because of eviction. */
    protected void notifyEviction(K key, V value) {
        if (evictionListener != null) {
            evictionListener.onEviction(key, value);
        }
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void setEvictionListener(EvictionListener<K, V> listener) {
        this.evictionListener = listener;
    }
}
