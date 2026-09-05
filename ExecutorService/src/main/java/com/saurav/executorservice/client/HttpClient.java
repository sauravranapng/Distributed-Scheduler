package com.saurav.executorservice.client;

import com.saurav.executorservice.model.payload.HttpJobPayload;
import org.springframework.http.ResponseEntity;

public interface HttpClient {
    ResponseEntity<String> execute(HttpJobPayload payload);
}
