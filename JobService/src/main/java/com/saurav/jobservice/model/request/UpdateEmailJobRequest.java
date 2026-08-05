package com.saurav.jobservice.model.request;

import com.saurav.jobservice.model.response.UpdateJobRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdateEmailJobRequest extends UpdateJobRequest {

    @Email
    @NotBlank
    private List<String> to;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;
}
