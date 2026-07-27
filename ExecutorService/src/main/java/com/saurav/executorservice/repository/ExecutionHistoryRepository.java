package com.saurav.executorservice.repository;

import com.saurav.executorservice.model.entity.ExecutionHistory;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExecutionHistoryRepository
        extends CassandraRepository<ExecutionHistory, UUID> {
}