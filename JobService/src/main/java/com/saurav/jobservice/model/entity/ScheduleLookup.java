package com.saurav.jobservice.model.entity;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Data
@Table("schedule_lookup")
public class ScheduleLookup {
    @PrimaryKey
    private UUID jobId;

    @Column("next_execution_time")
    private long nextExecutionTime;

    private int segment;
}
