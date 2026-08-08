package com.saurav.schedulingservice.model.entity;

import com.saurav.schedulingservice.model.enums.JobType;
import com.saurav.schedulingservice.model.primarykey.TaskSchedulePrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;


@Data
@Builder
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

    @Column("job_payload")
    private String jobPayload;

    private boolean recurring;

    private String interval;

    @Column("remaining_executions")
    private Integer remainingExecutions;

    @Column("end_time")
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Instant endTime;
}