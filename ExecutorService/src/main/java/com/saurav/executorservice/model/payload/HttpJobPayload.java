package com.saurav.executorservice.model.payload;

import lombok.Data;

import java.util.Map;

@Data
public class HttpJobPayload implements JobPayload {

    private String method;

    private String url;

    private Map<String, String> headers;

    private String body;

    private Integer timeoutSeconds;

}