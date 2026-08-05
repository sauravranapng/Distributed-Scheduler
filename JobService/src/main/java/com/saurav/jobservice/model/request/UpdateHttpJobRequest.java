package com.saurav.jobservice.model.request;

import com.saurav.jobservice.model.response.UpdateJobRequest;
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
public class UpdateHttpJobRequest extends UpdateJobRequest {

    @NotNull
    private HttpMethod method;

    @NotBlank
    private String url;

    private Map<String, String> headers = new HashMap<>();

    private String body;

    private Integer timeoutSeconds;
}