package com.saurav.schedulingservice.mapper;

import com.saurav.schedulingservice.model.entity.TaskSchedule;
import com.saurav.schedulingservice.model.enums.JobType;
import com.saurav.schedulingservice.model.primarykey.TaskSchedulePrimaryKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskScheduleMapperTest {

    private final TaskScheduleMapper mapper =
            new TaskScheduleMapper();

    @Test
    void copyWithNextExecutionTime_shouldCopyAllFields() {

        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Instant endTime =
                Instant.parse("2026-09-10T10:00:00Z");

        TaskSchedule original = TaskSchedule.builder()
                .key(new TaskSchedulePrimaryKey(
                        100L,
                        5,
                        jobId
                ))
                .userId(userId)
                .jobType(JobType.HTTP)
                .jobPayload("{\"url\":\"https://example.com\"}")
                .recurring(true)
                .interval("PT5M")
                .remainingExecutions(4)
                .endTime(endTime)
                .build();

        TaskSchedule result =
                mapper.copyWithNextExecutionTime(
                        original,
                        105L,
                        3
                );

        assertNotNull(result);

        assertEquals(
                105L,
                result.getKey().getNextExecutionTime()
        );

        assertEquals(
                5,
                result.getKey().getSegment()
        );

        assertEquals(
                jobId,
                result.getKey().getJobId()
        );

        assertEquals(userId, result.getUserId());
        assertEquals(JobType.HTTP, result.getJobType());
        assertEquals(
                "{\"url\":\"https://example.com\"}",
                result.getJobPayload()
        );
        assertTrue(result.isRecurring());
        assertEquals("PT5M", result.getInterval());
        assertEquals(3, result.getRemainingExecutions());
        assertEquals(endTime, result.getEndTime());
    }

    @Test
    void copyWithNextExecutionTime_shouldNotModifyOriginal() {

        UUID jobId = UUID.randomUUID();

        TaskSchedule original = TaskSchedule.builder()
                .key(new TaskSchedulePrimaryKey(
                        100L,
                        5,
                        jobId
                ))
                .remainingExecutions(4)
                .build();

        TaskSchedule result =
                mapper.copyWithNextExecutionTime(
                        original,
                        105L,
                        3
                );

        assertEquals(
                100L,
                original.getKey().getNextExecutionTime()
        );

        assertEquals(
                4,
                original.getRemainingExecutions()
        );

        assertEquals(
                105L,
                result.getKey().getNextExecutionTime()
        );

        assertEquals(
                3,
                result.getRemainingExecutions()
        );
    }

    @Test
    void copyWithNextExecutionTime_shouldAllowNullRemainingExecutions() {

        UUID jobId = UUID.randomUUID();

        TaskSchedule original = TaskSchedule.builder()
                .key(new TaskSchedulePrimaryKey(
                        100L,
                        2,
                        jobId
                ))
                .recurring(true)
                .build();

        TaskSchedule result =
                mapper.copyWithNextExecutionTime(
                        original,
                        110L,
                        null
                );

        assertNull(result.getRemainingExecutions());
        assertEquals(
                110L,
                result.getKey().getNextExecutionTime()
        );
    }
}