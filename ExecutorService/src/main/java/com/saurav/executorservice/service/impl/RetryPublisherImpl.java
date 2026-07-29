package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.exception.RetryPublishException;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.RetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryPublisherImpl implements RetryPublisher {

    private final KafkaTemplate<String, JobExecutionEvent> kafkaTemplate;

    @Value("${app.kafka.retry-topic}")
    private String retryTopic;

    @Override
    public void publish(JobExecutionEvent event, Exception exception) {

        try {

            kafkaTemplate.send(
                    retryTopic,
                    event.getExecutionId().toString(),
                    event
            ).get();   // <-- blocks
 //Without .get() producer buffer and immediately returns
            log.info("Published retry event. executionId={}, topic={}",
                    event.getExecutionId(),
                    retryTopic);

        } catch (Exception ex) {

            throw new RetryPublishException(
                    String.format("Failed to publish retry event for executionId=%s", event.getExecutionId()),
                    ex);
        }
    }
}