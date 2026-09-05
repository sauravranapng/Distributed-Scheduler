package com.saurav.executorservice.executor.impl;

import com.saurav.executorservice.client.HttpClient;
import com.saurav.executorservice.exception.NonRetryableHttpException;
import com.saurav.executorservice.exception.PayloadDeserializationException;
import com.saurav.executorservice.exception.RetryableExecutionException;
import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.payload.HttpJobPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor
@Slf4j
public class HttpJobExecutor implements JobExecutor {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.executor.test.delay-ms}")
    private long testDelayMs;

    @Override
    public JobType supportedType() {
        return JobType.HTTP;
    }

    @Override
    public void execute(JobExecutionEvent event) {

        if (testDelayMs > 0) {
            try {
                Thread.sleep(testDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RetryableExecutionException(
                        "Executor thread was interrupted", ex);
            }
        }

        HttpJobPayload payload;

        try {
            payload = objectMapper.readValue(
                    event.getJobPayload(),
                    HttpJobPayload.class);

        } catch (Exception ex) {

            throw new PayloadDeserializationException("Failed to deserialize HTTP job payload", ex);
        }

        httpClient.execute(payload);

        log.info(
                "HTTP job executed successfully. executionId={}, jobId={}",
                event.getExecutionId(),
                event.getJobId());

    }
}