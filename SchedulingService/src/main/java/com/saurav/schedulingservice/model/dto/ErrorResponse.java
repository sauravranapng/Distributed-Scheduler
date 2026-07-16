package com.saurav.schedulingservice.model.dto;


import java.time.Instant;

public record ErrorResponse(Instant timestamp, String message, String details) {
}