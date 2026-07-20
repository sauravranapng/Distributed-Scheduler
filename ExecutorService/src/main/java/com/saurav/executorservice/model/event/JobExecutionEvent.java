package com.saurav.executorservice.model.event;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionEvent {

    private UUID executionId;

    private UUID userId;

    private UUID jobId;

    private long scheduledExecutionTime;

}
