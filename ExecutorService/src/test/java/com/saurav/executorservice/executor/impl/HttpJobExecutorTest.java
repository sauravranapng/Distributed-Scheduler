package com.saurav.executorservice.executor.impl;

import com.saurav.executorservice.client.HttpClient;
import com.saurav.executorservice.exception.PayloadDeserializationException;
import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.payload.HttpJobPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.saurav.executorservice.exception.RetryableExecutionException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HttpJobExecutorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpClient httpClient;

    private HttpJobExecutor httpJobExecutor;

    @BeforeEach
    void setUp() {

        httpJobExecutor =
                new HttpJobExecutor(
                        objectMapper,
                        httpClient);

        ReflectionTestUtils.setField(
                httpJobExecutor,
                "testDelayMs",
                0L);
    }

    @Test
    void supportedType_shouldReturnHttp() {

        assertEquals(
                JobType.HTTP,
                httpJobExecutor.supportedType());
    }

    @Test
    void execute_shouldDeserializeAndExecuteHttpClient()
            throws Exception {

        JobExecutionEvent event =
                buildEvent(
                        """
                        {
                          "method": "GET",
                          "url": "http://example.com"
                        }
                        """);

        HttpJobPayload payload =
                new HttpJobPayload();

        payload.setMethod("GET");
        payload.setUrl("http://example.com");

        when(objectMapper.readValue(
                event.getJobPayload(),
                HttpJobPayload.class))
                .thenReturn(payload);

        httpJobExecutor.execute(event);

        verify(objectMapper).readValue(
                event.getJobPayload(),
                HttpJobPayload.class);

        verify(httpClient).execute(payload);
    }

    @Test
    void execute_shouldThrowPayloadDeserializationExceptionForInvalidPayload()
            throws Exception {

        JobExecutionEvent event =
                buildEvent("invalid-json");

        RuntimeException parsingException =
                new RuntimeException("Invalid JSON");

        when(objectMapper.readValue(
                event.getJobPayload(),
                HttpJobPayload.class))
                .thenThrow(parsingException);

        PayloadDeserializationException exception =
                assertThrows(
                        PayloadDeserializationException.class,
                        () -> httpJobExecutor.execute(event));

        assertEquals(
                "Failed to deserialize HTTP job payload",
                exception.getMessage());

        assertEquals(
                parsingException,
                exception.getCause());

        verifyNoInteractions(httpClient);
    }

    @Test
    void execute_shouldPropagateRetryableException()
            throws Exception {

        JobExecutionEvent event =
                buildEvent("{}");

        HttpJobPayload payload =
                new HttpJobPayload();

        payload.setMethod("GET");
        payload.setUrl("http://example.com");

        when(objectMapper.readValue(
                event.getJobPayload(),
                HttpJobPayload.class))
                .thenReturn(payload);

        RetryableExecutionException exception =
                new RetryableExecutionException(
                        "HTTP server error",
                        null);

        doThrow(exception)
                .when(httpClient)
                .execute(payload);

        RetryableExecutionException thrown =
                assertThrows(
                        RetryableExecutionException.class,
                        () -> httpJobExecutor.execute(event));

        assertEquals(
                exception,
                thrown);

        verify(httpClient)
                .execute(payload);
    }

    @Test
    void execute_shouldNotCallHttpClientWhenPayloadDeserializationFails()
            throws Exception {

        JobExecutionEvent event =
                buildEvent("invalid-json");

        when(objectMapper.readValue(
                event.getJobPayload(),
                HttpJobPayload.class))
                .thenThrow(
                        new RuntimeException("Invalid payload"));

        assertThrows(
                PayloadDeserializationException.class,
                () -> httpJobExecutor.execute(event));

        verifyNoInteractions(httpClient);
    }

    private JobExecutionEvent buildEvent(String payload) {

        return JobExecutionEvent.builder()
                .executionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jobId(UUID.randomUUID())
                .scheduledExecutionTime(Instant.now())
                .jobType(JobType.HTTP)
                .jobPayload(payload)
                .build();
    }
}