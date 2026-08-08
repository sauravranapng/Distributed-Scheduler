package com.saurav.schedulingservice.model.event;


import com.saurav.schedulingservice.model.enums.JobType;
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
public class JobExecutionEvent {

    private UUID executionId;

    private UUID userId;

    private UUID jobId;

    private JobType jobType;

    private String jobPayload;

    private Instant scheduledExecutionTime;
}