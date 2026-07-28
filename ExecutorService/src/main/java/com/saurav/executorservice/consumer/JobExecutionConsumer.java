package com.saurav.executorservice.consumer;

import com.saurav.executorservice.config.ExecutionCache;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.JobExecutionService;
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

        jobExecutionService.execute(event);

        acknowledgment.acknowledge();
    }
}