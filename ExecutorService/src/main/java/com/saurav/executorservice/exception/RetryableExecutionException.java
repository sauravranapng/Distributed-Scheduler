package com.saurav.executorservice.exception;

public class RetryableExecutionException extends RuntimeException {

    public RetryableExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}