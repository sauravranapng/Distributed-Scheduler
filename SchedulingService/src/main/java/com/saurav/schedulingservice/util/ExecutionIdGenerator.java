package com.saurav.schedulingservice.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ExecutionIdGenerator {

    private ExecutionIdGenerator() {
    }

    /**
     * Generates a deterministic execution ID for a scheduled execution.
     *
     * The same job scheduled for the same execution time will always
     * produce the same UUID.
     *
     * @param jobId the unique job identifier
     * @param scheduledExecutionTime execution time in epoch minutes
     * @return deterministic execution UUID
     */
    public static UUID generate(UUID jobId, long scheduledExecutionTime) {

        String source = jobId + ":" + scheduledExecutionTime;

        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }
}