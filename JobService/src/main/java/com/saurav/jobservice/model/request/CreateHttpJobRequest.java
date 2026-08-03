package com.saurav.jobservice.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateHttpJobRequest extends ScheduleRequest {

    @NotNull
    private HttpMethod method;

    @NotBlank
    private String url;

    /**
     * Optional HTTP headers.
     * Can also be used for Authorization, API Keys, etc.
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * Optional request body.
     * Supports JSON, XML, plain text, GraphQL, etc.
     */
    private String body;

    /**
     * Request timeout in seconds.
     * Null -> use application default.
     */
    private Integer timeoutSeconds;
}