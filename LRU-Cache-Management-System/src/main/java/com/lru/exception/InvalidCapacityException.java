package com.lru.exception;

/** Thrown when a requested capacity is not a positive integer. */
public class InvalidCapacityException extends CacheException {
    public InvalidCapacityException(String message) {
        super(message);
    }
}
