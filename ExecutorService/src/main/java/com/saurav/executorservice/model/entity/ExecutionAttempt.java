package com.saurav.executorservice.model.entity;


import com.saurav.executorservice.model.enums.ExecutionStatus;
import com.saurav.executorservice.model.primarykey.ExecutionAttemptPrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("task_execution_attempt")
public class ExecutionAttempt {

    @PrimaryKey
    private ExecutionAttemptPrimaryKey primaryKey;

    @Column("started_at")
    private Instant startedAt;

    @Column("completed_at")
    private Instant completedAt;

    @Column("execution_status")
    private ExecutionStatus executionStatus;

    /**
     * Time taken by this attempt in milliseconds.
     */
    @Column("duration_ms")
    private Long durationMs;

    /**
     * Retry topic name.
     * Null for initial execution.
     */
    @Column("retry_topic")
    private String retryTopic;

    /**
     * Failure reason if execution failed.
     */
    @Column("error_message")
    private String errorMessage;

    @Column("lease_until")
    private Instant leaseUntil;

}