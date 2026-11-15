package com.lru.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Least Recently Used cache: HashMap + doubly linked list.
 *
 * <pre>
 *   head (sentinel) <-> [MRU] <-> ... <-> [LRU] <-> tail (sentinel)
 *
 *   HashMap  : key -> Node          => O(1) lookup
 *   List     : usage order          => O(1) move-to-front / evict-from-back
 * </pre>
 *
 * Sentinel nodes remove every "is this the first/last node?" special case.
 * Not thread-safe; wrap with external synchronization if shared between threads.
 */
public class LRUCache<K, V> extends AbstractCache<K, V> {

    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);

    public LRUCache(int capacity) {
        super(capacity);
        head.next = tail;
        tail.prev = head;
    }

    /** O(1). New or updated entries become the most recently used; overflow evicts the LRU entry. */
    @Override
    public V put(K key, V value) {
        requireKey(key);
        requireValue(value);

        Node<K, V> existing = map.get(key);
        if (existing != null) {
            V old = existing.value;
            existing.value = value;
            moveToFront(existing);
            return old;
        }

        Node<K, V> node = new Node<>(key, value);
        map.put(key, node);
        addToFront(node);
        if (map.size() > capacity) {
            evictLeastRecentlyUsed();
        }
        return null;
    }

    /** O(1). A hit moves the node to the front. */
    @Override
    public V get(K key) {
        requireKey(key);
        Node<K, V> node = map.get(key);
        if (node == null) {
            return null;
        }
        moveToFront(node);
        return node.value;
    }

    /** O(1). Reads without touching the list, so usage order is unchanged. */
    @Override
    public V peek(K key) {
        requireKey(key);
        Node<K, V> node = map.get(key);
        return node == null ? null : node.value;
    }

    /** O(1). */
    @Override
    public V remove(K key) {
        requireKey(key);
        Node<K, V> node = map.remove(key);
        if (node == null) {
            return null;
        }
        unlink(node);
        return node.value;
    }

    /** O(n): drops every mapping. */
    @Override
    public void clear() {
        map.clear();
        head.next = tail;
        tail.prev = head;
    }

    /** O(k) where k is the number of entries evicted; shrinking evicts from the LRU end. */
    @Override
    public void resize(int newCapacity) {
        validateCapacity(newCapacity);
        this.capacity = newCapacity;
        while (map.size() > capacity) {
            evictLeastRecentlyUsed();
        }
    }

    @Override
    public boolean containsKey(K key) {
        requireKey(key);
        return map.containsKey(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    /** O(n). Most recently used first, least recently used last. */
    @Override
    public List<CacheEntry<K, V>> entries() {
        List<CacheEntry<K, V>> result = new ArrayList<>(map.size());
        for (Node<K, V> n = head.next; n != tail; n = n.next) {
            result.add(new CacheEntry<>(n.getKey(), n.getValue()));
        }
        return result;
    }

    @Override
    public String policyName() {
        return "LRU";
    }

    // ---- linked list primitives: every one is O(1) pointer rewiring ----

    private void addToFront(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void unlink(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    private void moveToFront(Node<K, V> node) {
        unlink(node);
        addToFront(node);
    }

    private void evictLeastRecentlyUsed() {
        Node<K, V> lru = tail.prev;   // the node just before the tail sentinel
        unlink(lru);
        map.remove(lru.key);
        notifyEviction(lru.key, lru.value);
    }
}
