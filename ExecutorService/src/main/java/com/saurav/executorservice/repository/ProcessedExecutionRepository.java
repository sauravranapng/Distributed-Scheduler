package com.saurav.executorservice.repository;

import java.util.UUID;

public interface ProcessedExecutionRepository {

    boolean tryAcquire(UUID executionId, UUID jobId);

}