package com.saurav.jobservice.model.response;

import com.saurav.jobservice.model.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobSummaryResponse {

    private UUID userId;

    private UUID jobId;

    private JobType jobType;

    private boolean recurring;

    private Instant startTime;

    private Instant createdTime;

    private String interval;

    private Instant endTime;
}