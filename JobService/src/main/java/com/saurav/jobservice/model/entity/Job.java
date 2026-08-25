package com.saurav.jobservice.model.entity;


import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("job_table")
public class Job {

        @PrimaryKey
        private JobPrimaryKey jobPrimaryKey;

        private boolean recurring;

        @Column("job_type")
        private JobType jobType;

        @Column("job_payload")
        @CassandraType(type = CassandraType.Name.TEXT)
        private String jobPayload;

        @Column("interval")
        @CassandraType(type = CassandraType.Name.TEXT)
        private String interval;

        @Column("max_executions")
        private Integer maxExecutions;

        @Column("start_time")
        @CassandraType(type = CassandraType.Name.TIMESTAMP)
        private Instant startTime;

        @Column("end_time")
        private Instant endTime;

        @Column("created_time")
        @CassandraType(type = CassandraType.Name.TIMESTAMP)
        private Instant createdTime; // Use TIMESTAMP for Instant

        @Column("updated_time")
        @CassandraType(type = CassandraType.Name.TIMESTAMP)
        private Instant updatedTime;

}
