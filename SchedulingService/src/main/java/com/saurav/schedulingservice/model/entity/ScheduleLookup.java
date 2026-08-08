package com.saurav.schedulingservice.model.entity;

import lombok.Builder;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Builder
@Table("schedule_lookup")
public class ScheduleLookup {

    @PrimaryKey
    private UUID jobId;

    @Column("next_execution_time")
    private long nextExecutionTime;

    private int segment;
}