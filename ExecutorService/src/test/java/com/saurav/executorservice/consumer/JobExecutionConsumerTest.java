package com.saurav.executorservice.consumer;

import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.service.JobExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobExecutionConsumerTest {

    @Mock
    private JobExecutionService jobExecutionService;

    private JobExecutionConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer =
                new JobExecutionConsumer(jobExecutionService);
    }

    @Test
    void consume_shouldPassEventAndAttemptToExecutionService() {

        JobExecutionEvent event = buildEvent();

        consumer.consume(event, 3);

        verify(jobExecutionService)
                .execute(event, 3);
    }

    @Test
    void consume_shouldPassInitialAttempt() {

        JobExecutionEvent event = buildEvent();

        consumer.consume(event, 1);

        verify(jobExecutionService)
                .execute(event, 1);
    }

    private JobExecutionEvent buildEvent() {

        return JobExecutionEvent.builder()
                .executionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jobId(UUID.randomUUID())
                .scheduledExecutionTime(Instant.now())
                .jobType(JobType.HTTP)
                .jobPayload("{}")
                .build();
    }
}