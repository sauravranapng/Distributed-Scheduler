package com.saurav.jobservice.model.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpJobPayload implements JobPayload{

    private HttpMethod method;

    private String url;

    private Map<String, String> headers;

    private String body;

    private Integer timeoutSeconds;
}