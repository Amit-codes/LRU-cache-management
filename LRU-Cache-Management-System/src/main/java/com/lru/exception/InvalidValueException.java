package com.lru.exception;

/** Thrown when a value is null or blank. */
public class InvalidValueException extends CacheException {
    public InvalidValueException(String message) {
        super(message);
    }
}
