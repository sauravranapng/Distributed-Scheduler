package com.saurav.executorservice.executor.impl;

import com.saurav.executorservice.exception.PayloadDeserializationException;
import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailJobExecutorTest {

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final EmailJobExecutor emailJobExecutor =
            new EmailJobExecutor(objectMapper);

    @Test
    void supportedType_shouldReturnEmail() {

        assertEquals(
                JobType.EMAIL,
                emailJobExecutor.supportedType());
    }

    @Test
    void execute_shouldDeserializeValidPayload() throws Exception {

        JobExecutionEvent event =
                buildEvent(
                        """
                        {
                          "to": "test@example.com",
                          "subject": "Test Subject",
                          "body": "Hello"
                        }
                        """);

        when(objectMapper.readValue(
                event.getJobPayload(),
                com.saurav.executorservice.model.payload.EmailJobPayload.class))
                .thenReturn(
                        createEmailPayload());

        assertDoesNotThrow(
                () -> emailJobExecutor.execute(event));
    }

    @Test
    void execute_shouldThrowPayloadDeserializationExceptionForInvalidPayload()
            throws Exception {

        JobExecutionEvent event =
                buildEvent(
                        "invalid-json");

        RuntimeException parsingException =
                new RuntimeException("Invalid JSON");

        when(objectMapper.readValue(
                event.getJobPayload(),
                com.saurav.executorservice.model.payload.EmailJobPayload.class))
                .thenThrow(parsingException);

        PayloadDeserializationException exception =
                assertThrows(
                        PayloadDeserializationException.class,
                        () -> emailJobExecutor.execute(event));

        assertEquals(
                "Failed to deserialize email payload",
                exception.getMessage());

        assertEquals(
                parsingException,
                exception.getCause());
    }

    private com.saurav.executorservice.model.payload.EmailJobPayload
    createEmailPayload() {

        com.saurav.executorservice.model.payload.EmailJobPayload payload =
                new com.saurav.executorservice.model.payload.EmailJobPayload();

        payload.setTo("test@example.com");
        payload.setSubject("Test Subject");
        payload.setBody("Hello");

        return payload;
    }

    private JobExecutionEvent buildEvent(
            String payload) {

        return JobExecutionEvent.builder()
                .executionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jobId(UUID.randomUUID())
                .scheduledExecutionTime(Instant.now())
                .jobType(JobType.EMAIL)
                .jobPayload(payload)
                .build();
    }
}