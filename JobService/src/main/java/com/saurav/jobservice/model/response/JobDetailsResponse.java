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
public abstract class JobDetailsResponse {

    private UUID userId;

    private UUID jobId;

    private JobType jobType;

    private boolean recurring;

    /**
     * ISO-8601 Duration
     */
    private String interval;

    private Integer maxExecutions;

    private Instant startTime;

    private Instant endTime;

    private Instant createdTime;
}