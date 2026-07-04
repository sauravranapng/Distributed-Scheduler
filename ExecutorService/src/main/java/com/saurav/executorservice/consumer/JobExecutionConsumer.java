package com.saurav.executorservice.consumer;

import com.saurav.executorservice.event.JobExecutionEvent;
import com.saurav.executorservice.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutionConsumer {

    private final JobExecutionService jobExecutionService;

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "executor-service"
    )
    public void consume(JobExecutionEvent event) {

        log.info("Received JobExecutionEvent : {}", event);

        jobExecutionService.execute(event);
    }
}