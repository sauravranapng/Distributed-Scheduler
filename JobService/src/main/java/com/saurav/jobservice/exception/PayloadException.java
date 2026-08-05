package com.saurav.jobservice.exception;

public abstract class PayloadException extends RuntimeException {

    protected PayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}