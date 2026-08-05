package com.saurav.jobservice.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailJobDetailsResponse extends JobDetailsResponse {

    private List<String> to;

    private String subject;

    private String body;
}