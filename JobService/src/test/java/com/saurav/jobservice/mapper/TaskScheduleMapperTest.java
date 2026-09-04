package com.saurav.jobservice.mapper;

import com.saurav.jobservice.model.entity.Job;
import com.saurav.jobservice.model.entity.TaskSchedule;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import com.saurav.jobservice.model.primarykey.TaskSchedulePrimaryKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskScheduleMapperTest {

    private final TaskScheduleMapper mapper = new TaskScheduleMapper();

    @Test
    void toTaskSchedule_shouldMapJobAndScheduleFields() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        Instant endTime = Instant.now().plus(2, ChronoUnit.HOURS);

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.HTTP)
                .jobPayload("{\"url\":\"https://example.com\"}")
                .recurring(true)
                .interval("PT5M")
                .maxExecutions(10)
                .endTime(endTime)
                .build();

        long nextExecutionTime = 123456L;
        int segment = 25;

        TaskSchedule result =
                mapper.toTaskSchedule(job, nextExecutionTime, segment);

        assertEquals(
                new TaskSchedulePrimaryKey(
                        nextExecutionTime,
                        segment,
                        jobId
                ),
                result.getKey()
        );

        assertEquals(userId, result.getUserId());
        assertEquals(JobType.HTTP, result.getJobType());
        assertEquals(job.getJobPayload(), result.getJobPayload());
        assertTrue(result.isRecurring());
        assertEquals("PT5M", result.getInterval());
        assertEquals(10, result.getRemainingExecutions());
        assertEquals(endTime, result.getEndTime());
    }
}