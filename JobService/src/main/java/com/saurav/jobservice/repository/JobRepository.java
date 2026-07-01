package com.saurav.jobservice.repository;


import com.saurav.jobservice.entity.Job;
import com.saurav.jobservice.util.JobPrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends CassandraRepository<Job, JobPrimaryKey> {
    Job findByJobPrimaryKey(JobPrimaryKey id);
    void deleteByJobPrimaryKey(JobPrimaryKey id);
    List<Job> findByJobPrimaryKeyUserId(UUID userId);
}
