package com.saurav.jobservice.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateEmailJobRequest extends ScheduleRequest {

    @Email
    @NotEmpty
    private List<String> to;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;
}