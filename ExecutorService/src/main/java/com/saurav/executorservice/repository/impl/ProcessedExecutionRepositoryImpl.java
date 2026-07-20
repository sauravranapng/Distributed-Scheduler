package com.saurav.executorservice.repository.impl;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.saurav.executorservice.repository.ProcessedExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedExecutionRepositoryImpl
        implements ProcessedExecutionRepository {

    private final CqlSession session;

    @Override
    public boolean tryAcquire(UUID executionId, UUID jobId) {

        SimpleStatement statement = SimpleStatement.builder("""
                INSERT INTO processed_execution
                (execution_id, job_id, processed_at)
                VALUES (?, ?, toTimestamp(now()))
                IF NOT EXISTS
                """)
                .addPositionalValues(
                        executionId,
                        jobId)
                .build();

        ResultSet resultSet = session.execute(statement);

        Row row = resultSet.one();

        return row != null && row.getBoolean("[applied]");
    }
}