package com.saurav.executorservice.model.primarykey;

import java.util.UUID;

public record ExecutionCacheKey(
        UUID executionId,
        int deliveryAttempt
) {
}