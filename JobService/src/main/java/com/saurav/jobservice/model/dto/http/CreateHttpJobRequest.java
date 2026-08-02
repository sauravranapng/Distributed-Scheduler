package com.saurav.jobservice.model.dto.http;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.Map;

public class CreateHttpJobRequest {

    @NotBlank
    private String url;

    @NotNull
    private HttpMethod method;

    private Map<String, String> headers;

    private Object body;

    private boolean recurring;

    private String interval;

    private Integer maxExecutions;

    private Instant endTime;
}