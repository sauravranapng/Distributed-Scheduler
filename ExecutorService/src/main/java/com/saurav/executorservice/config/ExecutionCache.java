package com.saurav.executorservice.config;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.UUID;

@Component
public class ExecutionCache {

    private final Cache<UUID, Boolean> cache =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(2))
                    .maximumSize(100_000)
                    .build();

    public boolean tryAcquireExecution(UUID executionId) {
        return cache.asMap().putIfAbsent(executionId, Boolean.TRUE) != null;
    }
}