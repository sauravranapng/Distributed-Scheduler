package com.saurav.jobservice.dto;

import com.saurav.jobservice.util.JobDtoPrimaryKey;
import lombok.*;

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
