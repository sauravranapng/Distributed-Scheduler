package com.saurav.executorservice.repository;

import com.saurav.executorservice.model.entity.ExecutionAttempt;
import com.saurav.executorservice.model.primarykey.ExecutionAttemptPrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ExecutionAttemptRepository
        extends CassandraRepository<ExecutionAttempt, ExecutionAttemptPrimaryKey> {
}