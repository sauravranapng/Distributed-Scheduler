package com.saurav.executorservice.client;

import com.saurav.executorservice.exception.NonRetryableHttpException;
import com.saurav.executorservice.exception.RetryableExecutionException;
import com.saurav.executorservice.model.payload.HttpJobPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class HttpClientImplTest {

    private HttpClientImpl httpClient;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {

        RestClient.Builder builder =
                RestClient.builder();

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        RestClient restClient = builder.build();

        httpClient = new HttpClientImpl(restClient);
    }

    @Test
    void execute_shouldSuccessfullyExecuteGetRequest() {

        HttpJobPayload payload = buildPayload();

        server.expect(
                        requestTo("https://example.com/test"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Test", "true"))
                .andRespond(
                        withStatus(HttpStatus.OK));

        httpClient.execute(payload);

        server.verify();
    }

    @Test
    void execute_shouldThrowNonRetryableExceptionFor4xx() {

        HttpJobPayload payload = buildPayload();

        server.expect(
                        requestTo("https://example.com/test"))
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST));

        NonRetryableHttpException exception =
                assertThrows(
                        NonRetryableHttpException.class,
                        () -> httpClient.execute(payload));

        assertEquals(
                "HTTP client error: 400 BAD_REQUEST",
                exception.getMessage());

        server.verify();
    }

    @Test
    void execute_shouldThrowRetryableExceptionFor5xx() {

        HttpJobPayload payload = buildPayload();

        server.expect(
                        requestTo("https://example.com/test"))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR));

        RetryableExecutionException exception =
                assertThrows(
                        RetryableExecutionException.class,
                        () -> httpClient.execute(payload));

        assertEquals(
                "HTTP server error: 500 INTERNAL_SERVER_ERROR",
                exception.getMessage());

        server.verify();
    }

    @Test
    void execute_shouldSendRequestBody() {

        HttpJobPayload payload = buildPayload();

        payload.setMethod("POST");
        payload.setBody("{\"name\":\"test\"}");

        server.expect(
                        requestTo("https://example.com/test"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.OK));

        httpClient.execute(payload);

        server.verify();
    }

    private HttpJobPayload buildPayload() {

        HttpJobPayload payload =
                new HttpJobPayload();

        payload.setMethod("GET");
        payload.setUrl(
                "https://example.com/test");

        payload.setHeaders(
                Map.of("X-Test", "true"));

        return payload;
    }
}