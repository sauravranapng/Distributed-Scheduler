package com.saurav.jobservice.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HttpJobDetailsResponse extends JobDetailsResponse {

    private HttpMethod method;

    private String url;

    private Map<String, String> headers;

    private String body;

    private Integer timeoutSeconds;
}