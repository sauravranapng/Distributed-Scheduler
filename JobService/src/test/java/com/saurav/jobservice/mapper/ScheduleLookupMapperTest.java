package com.saurav.jobservice.mapper;

import com.saurav.jobservice.model.entity.ScheduleLookup;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleLookupMapperTest {

    private final ScheduleLookupMapper mapper =
            new ScheduleLookupMapper();

    @Test
    void toEntity_shouldMapAllFields() {
        UUID jobId = UUID.randomUUID();
        long nextExecutionTime = 123456L;
        int segment = 42;

        ScheduleLookup result =
                mapper.toEntity(jobId, nextExecutionTime, segment);

        assertEquals(jobId, result.getJobId());
        assertEquals(nextExecutionTime, result.getNextExecutionTime());
        assertEquals(segment, result.getSegment());
    }
}