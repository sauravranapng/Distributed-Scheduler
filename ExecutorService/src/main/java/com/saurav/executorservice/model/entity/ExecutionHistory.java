package com.saurav.executorservice.model.entity;

import com.saurav.executorservice.model.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
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
    @Column("execution_id")
    private UUID executionId;

    @Column("job_id")
    private UUID jobId;

    @Column("user_id")
    private UUID userId;

    /**
     * Epoch seconds for which this execution was scheduled.
     */
    @Column("scheduled_execution_time")
    private long scheduledExecutionTime;

    @Column("execution_status")
    private ExecutionStatus executionStatus;

    @Column("started_at")
    private Instant startedAt;

    @Column("completed_at")
    private Instant completedAt;

    @Column("last_attempt_time")
    private Instant lastAttemptTime;

    /**
     * Total attempts made for this execution.
     */
    @Column("total_attempts")
    private Integer totalAttempts;

    /**
     * Last error encountered.
     */
    @Column("error_message")
    private String errorMessage;

}