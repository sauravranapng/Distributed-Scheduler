package com.saurav.jobservice.mapper;


import com.saurav.jobservice.model.entity.Job;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.payload.EmailJobPayload;
import com.saurav.jobservice.model.payload.HttpJobPayload;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.response.EmailJobDetailsResponse;
import com.saurav.jobservice.model.response.HttpJobDetailsResponse;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.model.response.JobSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobMapperTest {

    private final JobMapper mapper = new JobMapper();

    @Test
    void toEntity_shouldMapAllFields() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Instant createdTime = Instant.now();

        CreateHttpJobRequest request = new CreateHttpJobRequest();
        request.setRecurring(true);
        request.setInterval("PT5M");
        request.setMaxExecutions(10);
        request.setEndTime(createdTime.plus(1, ChronoUnit.HOURS));

        JobPrimaryKey key = new JobPrimaryKey(userId, jobId);

        Job result = mapper.toEntity(
                key,
                JobType.HTTP,
                "{\"url\":\"https://example.com\"}",
                request,
                createdTime
        );

        assertEquals(key, result.getJobPrimaryKey());
        assertEquals(JobType.HTTP, result.getJobType());
        assertEquals("{\"url\":\"https://example.com\"}", result.getJobPayload());
        assertTrue(result.isRecurring());
        assertEquals("PT5M", result.getInterval());
        assertEquals(10, result.getMaxExecutions());
        assertEquals(createdTime.plus(1, ChronoUnit.HOURS), result.getEndTime());
        assertEquals(createdTime, result.getCreatedTime());
    }

    @Test
    void toResponse_shouldMapJob() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Instant createdTime = Instant.now();

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.HTTP)
                .recurring(true)
                .interval("PT5M")
                .maxExecutions(5)
                .endTime(createdTime.plus(1, ChronoUnit.HOURS))
                .createdTime(createdTime)
                .build();

        JobResponse result = mapper.toResponse(job);

        assertEquals(userId, result.getUserId());
        assertEquals(jobId, result.getJobId());
        assertEquals(JobType.HTTP, result.getJobType());
        assertTrue(result.isRecurring());
        assertEquals("PT5M", result.getInterval());
        assertEquals(5, result.getMaxExecutions());
        assertEquals(createdTime.plus(1, ChronoUnit.HOURS), result.getEndTime());
        assertEquals(createdTime, result.getCreatedTime());
    }

    @Test
    void toHttpJobResponse_shouldMapPayload() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.HTTP)
                .recurring(false)
                .build();

        HttpJobPayload payload = HttpJobPayload.builder()
                .method("POST")
                .url("https://example.com")
                .headers(Map.of("Content-Type", "application/json"))
                .body("{\"test\":true}")
                .timeoutSeconds(30)
                .build();

        HttpJobDetailsResponse result =
                mapper.toHttpJobResponse(job, payload);

        assertEquals(userId, result.getUserId());
        assertEquals(jobId, result.getJobId());
        assertEquals(JobType.HTTP, result.getJobType());
        assertEquals(HttpMethod.POST, result.getMethod());
        assertEquals("https://example.com", result.getUrl());
        assertEquals("application/json", result.getHeaders().get("Content-Type"));
        assertEquals("{\"test\":true}", result.getBody());
        assertEquals(30, result.getTimeoutSeconds());
    }

    @Test
    void toEmailJobResponse_shouldMapPayload() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.EMAIL)
                .build();

        EmailJobPayload payload = EmailJobPayload.builder()
                .to(List.of("test@example.com"))
                .subject("Test")
                .body("Hello")
                .build();

        EmailJobDetailsResponse result =
                mapper.toEmailJobResponse(job, payload);

        assertEquals(userId, result.getUserId());
        assertEquals(jobId, result.getJobId());
        assertEquals(JobType.EMAIL, result.getJobType());
        assertEquals(List.of("test@example.com"), result.getTo());
        assertEquals("Test", result.getSubject());
        assertEquals("Hello", result.getBody());
    }

    @Test
    void toJobSummaryResponse_shouldMapJob() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.EMAIL)
                .recurring(true)
                .interval("PT10M")
                .startTime(now)
                .endTime(now.plus(1, ChronoUnit.HOURS))
                .createdTime(now)
                .build();

        JobSummaryResponse result =
                mapper.toJobSummaryResponse(job);

        assertEquals(userId, result.getUserId());
        assertEquals(jobId, result.getJobId());
        assertEquals(JobType.EMAIL, result.getJobType());
        assertTrue(result.isRecurring());
        assertEquals("PT10M", result.getInterval());
        assertEquals(now, result.getStartTime());
        assertEquals(now.plus(1, ChronoUnit.HOURS), result.getEndTime());
        assertEquals(now, result.getCreatedTime());
    }
}