package com.saurav.jobservice.model.entity;

import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.primarykey.TaskSchedulePrimaryKey;
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
@AllArgsConstructor
@NoArgsConstructor
@Table("task_schedule")
public class TaskSchedule{

    @PrimaryKey
    private TaskSchedulePrimaryKey key;

    @Column("user_id")
    private UUID userId;

    private JobType jobType;

    private String payload;

    private boolean recurring;

    private String interval;

    private Integer remainingExecutions;

    private Instant endTime;

}