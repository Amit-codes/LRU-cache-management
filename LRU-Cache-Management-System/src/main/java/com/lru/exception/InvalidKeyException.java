package com.lru.exception;

/** Thrown when a key is null, empty or blank. */
public class InvalidKeyException extends CacheException {
    public InvalidKeyException(String message) {
        super(message);
    }
}
