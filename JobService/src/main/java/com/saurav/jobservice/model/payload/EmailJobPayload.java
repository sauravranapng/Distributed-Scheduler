package com.saurav.jobservice.model.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailJobPayload implements JobPayload {

    private List<String> to;

    private String subject;

    private String body;
}