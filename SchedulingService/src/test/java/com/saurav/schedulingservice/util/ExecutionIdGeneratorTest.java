package com.saurav.schedulingservice.util;


import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionIdGeneratorTest {

    @Test
    void generate_shouldReturnDeterministicExecutionId() {
        UUID jobId = UUID.randomUUID();
        long executionTime = 123456L;

        UUID first =
                ExecutionIdGenerator.generate(jobId, executionTime);

        UUID second =
                ExecutionIdGenerator.generate(jobId, executionTime);

        assertEquals(first, second);
    }

    @Test
    void generate_shouldReturnDifferentIdForDifferentJobs() {
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();

        long executionTime = 123456L;

        UUID first =
                ExecutionIdGenerator.generate(jobId1, executionTime);

        UUID second =
                ExecutionIdGenerator.generate(jobId2, executionTime);

        assertNotEquals(first, second);
    }

    @Test
    void generate_shouldReturnDifferentIdForDifferentExecutionTimes() {
        UUID jobId = UUID.randomUUID();

        UUID first =
                ExecutionIdGenerator.generate(jobId, 100L);

        UUID second =
                ExecutionIdGenerator.generate(jobId, 101L);

        assertNotEquals(first, second);
    }

    @Test
    void generate_shouldReturnNonNullId() {
        UUID result =
                ExecutionIdGenerator.generate(
                        UUID.randomUUID(),
                        100L
                );

        assertNotNull(result);
    }
}