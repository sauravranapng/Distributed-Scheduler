package com.saurav.executorservice.exception;

public class NonRetryableHttpException
        extends RuntimeException {

    public NonRetryableHttpException(String message) {
        super(message);
    }
}