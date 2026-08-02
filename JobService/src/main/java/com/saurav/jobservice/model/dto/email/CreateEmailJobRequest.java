package com.saurav.jobservice.model.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class CreateEmailJobRequest {

    @Email
    private String to;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    private boolean recurring;

    private String interval;

    private Integer maxExecutions;

    private Instant endTime;
}