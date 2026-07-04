package com.saurav.executorservice.repository;


import com.saurav.executorservice.entity.Job;
import com.saurav.executorservice.entity.JobPrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends CassandraRepository<Job, JobPrimaryKey> {
    Job findByJobPrimaryKey(JobPrimaryKey id);
}
