package com.saurav.executorservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

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
