package com.saurav.executorservice.service;

public interface RetryPolicy {

    boolean shouldRetry(Exception exception);
}