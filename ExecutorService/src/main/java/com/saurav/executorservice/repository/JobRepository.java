package com.saurav.executorservice.repository;



import com.saurav.executorservice.model.entity.Job;
import com.saurav.executorservice.model.primarykey.JobPrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface JobRepository extends CassandraRepository<Job, JobPrimaryKey> {
    Job findByJobPrimaryKey(JobPrimaryKey id);
}
