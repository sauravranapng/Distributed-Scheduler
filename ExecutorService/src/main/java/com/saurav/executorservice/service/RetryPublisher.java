package com.saurav.executorservice.service;

import com.saurav.executorservice.model.event.JobExecutionEvent;

public interface RetryPublisher {

    void publish(JobExecutionEvent event, Exception exception);
}