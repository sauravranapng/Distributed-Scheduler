package com.saurav.jobservice.model.response;

import com.saurav.jobservice.model.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobResponse {

    private UUID jobId;

    private JobType jobType;

    private boolean recurring;

    private Instant createdTime;
}