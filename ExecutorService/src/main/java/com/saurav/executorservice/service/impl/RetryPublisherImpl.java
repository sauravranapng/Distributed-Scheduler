package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.exception.RetryPublishException;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.RetryPolicy;
import com.saurav.executorservice.service.RetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryPublisherImpl implements RetryPublisher {

    private final KafkaTemplate<String, JobExecutionEvent> kafkaTemplate;
    private final RetryPolicy retryPolicy;


    @Override
    public void publish(JobExecutionEvent event, Exception exception) {
        JobExecutionEvent retryEvent = new JobExecutionEvent(
                event.getExecutionId(),
                event.getUserId(),
                event.getJobId(),
                event.getScheduledExecutionTime(),
                event.getCurrentAttempt() + 1
        );
        String retryTopic = retryPolicy.getRetryTopic(retryEvent);

        try {

            kafkaTemplate.send(
                    retryTopic,
                    retryEvent.getExecutionId().toString(),
                    retryEvent
            ).get();   // <-- blocks
 //Without .get() producer buffer and immediately returns
            log.info("Published retry attempt {} for executionId={} to topic={}",
                    retryEvent.getCurrentAttempt(),
                    retryEvent.getExecutionId(),
                    retryTopic);

        } catch (Exception ex) {

            throw new RetryPublishException(
                    String.format("Failed to publish retry event for executionId=%s", event.getExecutionId()),
                    ex);
        }
    }
}