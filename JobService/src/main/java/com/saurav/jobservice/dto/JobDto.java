package com.saurav.jobservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobDto {

    private JobDtoPrimaryKey jobDtoPrimaryKey;
    private String description;
    private boolean recurring;
    private String interval;
    private int maxRetryCount;
    private Instant createdTime;
}
