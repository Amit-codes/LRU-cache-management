package com.lru.exception;

/** Base type for every error raised by the cache system. Unchecked so callers are not forced to catch. */
public class CacheException extends RuntimeException {
    public CacheException(String message) {
        super(message);
    }
}
