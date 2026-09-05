package com.saurav.executorservice.client;

import com.saurav.executorservice.exception.NonRetryableHttpException;
import com.saurav.executorservice.exception.RetryableExecutionException;
import com.saurav.executorservice.model.payload.HttpJobPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class HttpClientImpl implements HttpClient {

    private final RestClient restClient;

    @Override
    public ResponseEntity<String> execute(HttpJobPayload payload) {

        HttpHeaders headers = new HttpHeaders();

        if (payload.getHeaders() != null) {
            payload.getHeaders().forEach(headers::add);
        }

        try {

            RestClient.RequestBodySpec request = restClient
                    .method(HttpMethod.valueOf(payload.getMethod()))
                    .uri(payload.getUrl())
                    .headers(httpHeaders ->
                            httpHeaders.addAll(headers));

            if (payload.getBody() != null) {
                request.body(payload.getBody());
            }

            return request
                    .retrieve()

                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (requestSpec, response) -> {

                                throw new NonRetryableHttpException(
                                        "HTTP client error: "
                                                + response.getStatusCode());
                            })

                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (requestSpec, response) -> {

                                throw new RetryableExecutionException(
                                        "HTTP server error: "
                                                + response.getStatusCode(),
                                        null);
                            })

                    .toEntity(String.class);

        } catch (ResourceAccessException ex) {

            throw new RetryableExecutionException(
                    "HTTP request failed due to network error",
                    ex);
        }
    }
}