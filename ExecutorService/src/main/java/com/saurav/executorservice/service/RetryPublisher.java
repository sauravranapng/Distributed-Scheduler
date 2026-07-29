package com.saurav.executorservice.service;

import com.saurav.executorservice.model.event.JobExecutionEvent;

public interface RetryPublisher {
    /**
     * It only returns after Kafka has acknowledged the send.
     * @param event
     * @param exception
     */
    void publish(JobExecutionEvent event, Exception exception);
}