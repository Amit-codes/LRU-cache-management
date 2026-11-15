package com.lru.cache;

/**
 * Node of the doubly linked list that records usage order.
 * Holding both {@code prev} and {@code next} is what lets us unlink a node in O(1)
 * once the HashMap has handed it to us - a singly linked list would need an O(n) walk
 * to find the predecessor.
 */
final class Node<K, V> {
    final K key;      // kept so that, on eviction, we can also delete the HashMap entry
    V value;
    Node<K, V> prev;
    Node<K, V> next;

    Node(K key, V value) {
        this.key = key;
        this.value = value;
    }

    K getKey() {
        return key;
    }

    V getValue() {
        return value;
    }
}
