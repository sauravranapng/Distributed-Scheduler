package com.saurav.executorservice.consumer;

import com.saurav.executorservice.config.ExecutionCache;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutionConsumer {

    private final ExecutionCache executionCache;

    private final JobExecutionService jobExecutionService;

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "executor-service"
    )
    public void consume(JobExecutionEvent event) {

        log.info("Received JobExecutionEvent : {}", event);

        if (!executionCache.tryAcquireExecution(event.getExecutionId())) {
            log.info("Duplicate execution ignored: {}", event.getExecutionId());
            return;
        }

        jobExecutionService.execute(event);
    }
}