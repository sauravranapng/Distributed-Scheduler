package com.saurav.executorservice.exception;

public class RetryPublishException extends RuntimeException {

    public RetryPublishException(String message) {
        super(message);
    }

    public RetryPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}