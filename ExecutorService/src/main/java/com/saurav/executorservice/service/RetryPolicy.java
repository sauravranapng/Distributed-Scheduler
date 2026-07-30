package com.saurav.executorservice.service;

import com.saurav.executorservice.model.event.JobExecutionEvent;

public interface RetryPolicy {

    boolean shouldRetry(JobExecutionEvent event, Exception exception);

    String getRetryTopic(JobExecutionEvent event);
}