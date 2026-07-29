package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.RetryPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RetryPublisherImpl implements RetryPublisher {

    @Override
    public void publish(JobExecutionEvent event, Exception exception) {

        log.info("Publishing retry for executionId={}, reason={}",
                event.getExecutionId(),
                exception.getMessage());

        // Kafka implementation comes next
    }
}