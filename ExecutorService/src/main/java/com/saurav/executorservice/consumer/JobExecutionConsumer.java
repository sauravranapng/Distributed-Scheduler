package com.saurav.executorservice.consumer;

import com.saurav.executorservice.config.ExecutionCache;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.JobExecutionService;
import com.saurav.executorservice.service.RetryPolicy;
import com.saurav.executorservice.service.RetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.Acknowledgment;


@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutionConsumer {

    private final ExecutionCache executionCache;

    private final JobExecutionService jobExecutionService;

    private final RetryPublisher retryPublisher;

    private final RetryPolicy retryPolicy;


    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(JobExecutionEvent event,Acknowledgment acknowledgment) {

        log.info("Received executionId={}, jobId={}, userId={}",
                event.getExecutionId(),
                event.getJobId(),
                event.getUserId());

        if (!executionCache.tryAcquireExecution(event.getExecutionId())) {
            log.info("Duplicate execution ignored: {}", event.getExecutionId());
            acknowledgment.acknowledge();
            return;
        }

        try {

            jobExecutionService.execute(event);

            acknowledgment.acknowledge();

        } catch (Exception ex) {

            if (retryPolicy.shouldRetry(event,ex)) {

                retryPublisher.publish(event, ex); //if it throws execution immediately exits the listener with No ACK --> Event will be redelivered.

                acknowledgment.acknowledge();

                return;
            }

            throw ex;
        }
    }
}