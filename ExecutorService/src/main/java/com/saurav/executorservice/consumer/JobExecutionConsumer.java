package com.saurav.executorservice.consumer;

import com.saurav.executorservice.config.ExecutionCache;
import com.saurav.executorservice.exception.JobNotFoundException;
import com.saurav.executorservice.exception.RetryableExecutionException;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.ExecutionTrackingService;
import com.saurav.executorservice.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutionConsumer {

    private final ExecutionCache executionCache;

    private final JobExecutionService jobExecutionService;

    /**
     * Consumes job execution events from the Kafka topic.
     * @param event
     * JobExecutionEvent
     * @param attempt
     * int
     */
    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(
                    delay = 60_000,
                    multiplier = 5.0
            ),
            include = RetryableExecutionException.class
    )
    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(JobExecutionEvent event, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int attempt) {

        log.info("Received executionId={}, jobId={}, userId={}", event.getExecutionId(), event.getJobId(), event.getUserId());

        if (!executionCache.tryAcquireExecution(event.getExecutionId(),attempt)) {
            log.info("Duplicate execution ignored: {}", event.getExecutionId());
            return;
        }

        jobExecutionService.execute(event,attempt);

    }

    /**
     * There is no dataBase update here because the executionTrackingService.failExecution() method is already called in the execute() method of JobExecutionServiceImpl class when an exception occurs. This method updates the execution status to FAILED in the database.
     * @param event
     * JobExecutionEvent object containing the executionId and jobId of the failed job execution.
     */
    @DltHandler
    public void handleDeadLetter(JobExecutionEvent event) {

        log.error("Retries exhausted. executionId={}, jobId={}",
                event.getExecutionId(),
                event.getJobId());

    }
}