package com.saurav.jobservice.model.dto;


import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ErrorResponse {
    private final Instant timestamp;
    private final String message;
    private final String details;

    public ErrorResponse(Instant timestamp, String message, String details) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
    }
}