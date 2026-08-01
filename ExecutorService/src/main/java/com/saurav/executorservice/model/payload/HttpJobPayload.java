package com.saurav.executorservice.model.payload;

import org.springframework.http.HttpMethod;

import java.util.Map;

public class HttpJobPayload implements JobPayload {

    private HttpMethod method;

    private String url;

    private Map<String,String> headers;

    private Object body;

}