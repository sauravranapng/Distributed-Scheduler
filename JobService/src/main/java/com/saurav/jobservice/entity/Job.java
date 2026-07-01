package com.saurav.jobservice.entity;

import com.saurav.jobservice.util.JobPrimaryKey;
import lombok.*;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("job_table")
public class Job {

        @PrimaryKey
        private JobPrimaryKey jobPrimaryKey;


        private boolean recurring;

        @CassandraType(type = CassandraType.Name.TEXT)
        private  String description;

        @CassandraType(type = CassandraType.Name.TEXT)
        private String interval;

        @Column("max_retry_count")
        private int maxRetryCount;

        @Column("created_time")
        @CassandraType(type = CassandraType.Name.TIMESTAMP)
        private Instant createdTime; // Use TIMESTAMP for Instant

}
