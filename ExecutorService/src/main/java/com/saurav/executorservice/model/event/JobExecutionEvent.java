package com.saurav.executorservice.model.event;


import com.saurav.executorservice.model.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionEvent {

    private UUID executionId;

    private UUID userId;

    private UUID jobId;

    private Instant scheduledExecutionTime;

    private JobType jobType;

    private String jobPayload;
}