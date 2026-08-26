package com.saurav.executorservice.executor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.executorservice.exception.NonRetryableHttpException;
import com.saurav.executorservice.exception.PayloadDeserializationException;
import com.saurav.executorservice.exception.RetryableExecutionException;
import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.payload.HttpJobPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;


@Component
@RequiredArgsConstructor
@Slf4j
public class HttpJobExecutor implements JobExecutor {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Override
    public JobType supportedType() {
        return JobType.HTTP;
    }

    @Override
    public void execute(JobExecutionEvent event) {

        HttpJobPayload payload;

        try {
            payload = objectMapper.readValue(
                    event.getJobPayload(),
                    HttpJobPayload.class);

        } catch (Exception ex) {

            throw new PayloadDeserializationException("Failed to deserialize HTTP job payload", ex);
        }

        HttpHeaders headers = new HttpHeaders();

        if (payload.getHeaders() != null) {
            payload.getHeaders().forEach(headers::add);
        }

        try {

            ResponseEntity<String> res = restClient
                    .method(HttpMethod.valueOf(payload.getMethod()))
                    .uri(payload.getUrl())
                    .headers(httpHeaders ->
                            httpHeaders.addAll(headers))
                    .body(payload.getBody())
                    .retrieve()

                    // 4xx → permanent failure
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {

                                throw new NonRetryableHttpException(
                                        "HTTP client error: "
                                                + response.getStatusCode());
                            })

                    // 5xx → transient failure
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {

                                throw new RetryableExecutionException(
                                        "HTTP server error: "
                                                + response.getStatusCode(),
                                        null);
                            })

                    .toEntity(String.class);

            log.info("HTTP job executed successfully. executionId={}, jobId={}, status={}",
                    event.getExecutionId(),
                    event.getJobId(),
                    res.getStatusCode());

        } catch (ResourceAccessException ex) {

            /*
             * Connection timeout, connection refused,
             * DNS/network related failures, etc.
             */
            throw new RetryableExecutionException("HTTP request failed due to network error", ex);
        }
    }
}