package com.saurav.executorservice.model.entity;


import com.saurav.executorservice.model.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("task_execution_history")
public class ExecutionHistory {

    @PrimaryKey
    private UUID executionId;

    private UUID jobId;

    private UUID userId;

    /**
     * Epoch seconds for which this execution was scheduled.
     */
    private long scheduledExecutionTime;

    private ExecutionStatus executionStatus;

    private Instant startedAt;

    private Instant completedAt;

    private Instant lastAttemptTime;

    /**
     * Total attempts made for this execution.
     */
    private Integer totalAttempts;

    /**
     * Last error encountered.
     */
    private String errorMessage;

}