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
        /*
         Returns null if the key was not present (it inserts it).
         Returns the existing value if the key was already present (it does not insert).
         */
        return cache.asMap().putIfAbsent(executionId, Boolean.TRUE) == null;
    }
}