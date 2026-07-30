package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.exception.JobNotFoundException;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.RetryPolicy;
import com.saurav.executorservice.util.RetryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultRetryPolicy implements RetryPolicy {

    private final RetryProperties retryProperties;


    @Override
    public boolean shouldRetry(JobExecutionEvent event, Exception exception) {

        if (event.getCurrentAttempt() >= retryProperties.getMaxAttempts()) {
            return false;
        }

        return !(exception instanceof JobNotFoundException);
    }

    @Override
    public String getRetryTopic(JobExecutionEvent event) {

        String topic = retryProperties.getTopics()
                .get(String.valueOf(event.getCurrentAttempt()));

        if (topic == null) {
            return retryProperties.getTopics().get("dlq");
        }

        return topic;
    }
}