package com.saurav.schedulingservice.model.entity;

import com.saurav.schedulingservice.model.enums.JobType;
import com.saurav.schedulingservice.model.primarykey.TaskSchedulePrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("task_schedule")
public class TaskSchedule {

    @PrimaryKey
    private TaskSchedulePrimaryKey key;

    @Column("user_id")
    private UUID userId;

    @Column("job_type")
    private JobType jobType;

    @Column("payload")
    private String payload;

    private boolean recurring;

    @Column("interval_seconds")
    private Long intervalSeconds;

    @Column("remaining_executions")
    private Integer remainingExecutions;

    @Column("end_time")
    private Instant endTime;
}