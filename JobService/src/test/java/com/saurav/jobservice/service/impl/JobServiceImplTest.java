package com.saurav.jobservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.jobservice.exception.PayloadDeserializationException;
import com.saurav.jobservice.exception.ResourceNotFoundException;
import com.saurav.jobservice.mapper.JobMapper;
import com.saurav.jobservice.mapper.ScheduleLookupMapper;
import com.saurav.jobservice.mapper.TaskScheduleMapper;
import com.saurav.jobservice.model.entity.Job;
import com.saurav.jobservice.model.entity.ScheduleLookup;
import com.saurav.jobservice.model.entity.TaskSchedule;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import com.saurav.jobservice.model.primarykey.TaskSchedulePrimaryKey;
import com.saurav.jobservice.model.request.CreateEmailJobRequest;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.response.HttpJobDetailsResponse;
import com.saurav.jobservice.model.response.JobDetailsResponse;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.model.response.JobSummaryResponse;
import com.saurav.jobservice.repository.JobRepository;
import com.saurav.jobservice.repository.ScheduleLookupRepository;
import com.saurav.jobservice.repository.TaskScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private TaskScheduleMapper taskScheduleMapper;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private TaskScheduleRepository taskScheduleRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ScheduleLookupMapper scheduleLookupMapper;

    @Mock
    private ScheduleLookupRepository scheduleLookupRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    private UUID userId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
    }

    @Test
    void createHttpJob_shouldCreateAndPersistJob(){
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setMethod(HttpMethod.POST);
        request.setUrl("https://example.com");
        request.setHeaders(Map.of("Content-Type", "application/json"));
        request.setBody("{\"test\":true}");
        request.setTimeoutSeconds(30);
        request.setRecurring(false);

        JobResponse expectedResponse = JobResponse.builder()
                .userId(userId)
                .jobId(jobId)
                .jobType(JobType.HTTP)
                .build();

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.HTTP)
                .build();

        TaskSchedule taskSchedule = new TaskSchedule();

        ScheduleLookup lookup = ScheduleLookup.builder()
                .jobId(jobId)
                .nextExecutionTime(100L)
                .segment(10)
                .build();

        when(jobMapper.toEntity(
                any(JobPrimaryKey.class),
                eq(JobType.HTTP),
                anyString(),
                eq(request),
                any(Instant.class)
        )).thenReturn(job);

        when(taskScheduleMapper.toTaskSchedule(
                any(Job.class),
                anyLong(),
                anyInt()
        )).thenReturn(taskSchedule);

        when(scheduleLookupMapper.toEntity(
                any(UUID.class),
                anyLong(),
                anyInt()
        )).thenReturn(lookup);

        when(jobMapper.toResponse(job))
                .thenReturn(expectedResponse);

        JobResponse result =
                jobService.createHttpJob(userId, request);

        assertEquals(expectedResponse, result);

        verify(jobRepository).save(job);
        verify(taskScheduleRepository).save(taskSchedule);
        verify(scheduleLookupRepository).save(lookup);
    }

    @Test
    void createEmailJob_shouldCreateAndPersistJob() {
        CreateEmailJobRequest request =
                new CreateEmailJobRequest();

        request.setTo(List.of("test@example.com"));
        request.setSubject("Test");
        request.setBody("Hello");
        request.setRecurring(false);

        JobResponse expectedResponse = JobResponse.builder()
                .userId(userId)
                .jobId(jobId)
                .jobType(JobType.EMAIL)
                .build();

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.EMAIL)
                .build();

        when(jobMapper.toEntity(
                any(JobPrimaryKey.class),
                eq(JobType.EMAIL),
                anyString(),
                eq(request),
                any(Instant.class)
        )).thenReturn(job);

        when(taskScheduleMapper.toTaskSchedule(
                any(Job.class),
                anyLong(),
                anyInt()
        )).thenReturn(new TaskSchedule());

        when(scheduleLookupMapper.toEntity(
                any(UUID.class),
                anyLong(),
                anyInt()
        )).thenReturn(
                ScheduleLookup.builder()
                        .jobId(jobId)
                        .build()
        );

        when(jobMapper.toResponse(job))
                .thenReturn(expectedResponse);

        JobResponse result =
                jobService.createEmailJob(userId, request);

        assertEquals(expectedResponse, result);

        verify(jobRepository).save(job);
        verify(taskScheduleRepository).save(any(TaskSchedule.class));
        verify(scheduleLookupRepository).save(any(ScheduleLookup.class));
    }

    @Test
    void getJob_shouldReturnHttpJob() throws Exception {
        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.HTTP)
                .jobPayload("{\"method\":\"GET\",\"url\":\"https://example.com\"}")
                .build();

        HttpJobDetailsResponse expected =
                new HttpJobDetailsResponse();

        when(jobRepository.findByJobPrimaryKey(
                new JobPrimaryKey(userId, jobId)))
                .thenReturn(job);

        when(objectMapper.readValue(
                job.getJobPayload(),
                com.saurav.jobservice.model.payload.HttpJobPayload.class
        )).thenReturn(
                com.saurav.jobservice.model.payload.HttpJobPayload.builder()
                        .method("GET")
                        .url("https://example.com")
                        .build()
        );

        when(jobMapper.toHttpJobResponse(
                eq(job),
                any()
        )).thenReturn(expected);

        JobDetailsResponse result =
                jobService.getJob(userId, jobId);

        assertSame(expected, result);

        verify(jobMapper).toHttpJobResponse(eq(job), any());
    }

    @Test
    void getJob_shouldThrowWhenJobDoesNotExist() {
        when(jobRepository.findByJobPrimaryKey(
                any(JobPrimaryKey.class)))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> jobService.getJob(userId, jobId)
        );

        verifyNoInteractions(objectMapper);
    }

    @Test
    void getJob_shouldThrowPayloadDeserializationException() throws Exception {
        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(JobType.HTTP)
                .jobPayload("invalid-json")
                .build();

        when(jobRepository.findByJobPrimaryKey(
                any(JobPrimaryKey.class)))
                .thenReturn(job);

        when(objectMapper.readValue(
                anyString(),
                eq(com.saurav.jobservice.model.payload.HttpJobPayload.class)
        )).thenThrow(
                new JsonProcessingException("Invalid JSON") {}
        );

        assertThrows(
                PayloadDeserializationException.class,
                () -> jobService.getJob(userId, jobId)
        );
    }

    @Test
    void getJobsByUser_shouldMapAllJobs() {
        Job first = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, UUID.randomUUID()))
                .jobType(JobType.HTTP)
                .build();

        Job second = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, UUID.randomUUID()))
                .jobType(JobType.EMAIL)
                .build();

        JobSummaryResponse firstResponse =
                new JobSummaryResponse();

        JobSummaryResponse secondResponse =
                new JobSummaryResponse();

        when(jobRepository.findByJobPrimaryKeyUserId(userId))
                .thenReturn(List.of(first, second));

        when(jobMapper.toJobSummaryResponse(first))
                .thenReturn(firstResponse);

        when(jobMapper.toJobSummaryResponse(second))
                .thenReturn(secondResponse);

        List<JobSummaryResponse> result =
                jobService.getJobsByUser(userId);

        assertEquals(2, result.size());
        assertSame(firstResponse, result.get(0));
        assertSame(secondResponse, result.get(1));
    }

    @Test
    void getJobsByUser_shouldReturnEmptyListWhenNoJobsExist() {
        when(jobRepository.findByJobPrimaryKeyUserId(userId))
                .thenReturn(List.of());

        List<JobSummaryResponse> result =
                jobService.getJobsByUser(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteJob_shouldDeleteScheduleLookupAndJob() {
        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .build();

        ScheduleLookup lookup = ScheduleLookup.builder()
                .jobId(jobId)
                .nextExecutionTime(100L)
                .segment(20)
                .build();

        TaskSchedulePrimaryKey key =
                new TaskSchedulePrimaryKey(
                        100L,
                        20,
                        jobId
                );

        TaskSchedule taskSchedule = TaskSchedule.builder()
                .key(key)
                .build();

        when(jobRepository.findByJobPrimaryKey(
                any(JobPrimaryKey.class)))
                .thenReturn(job);

        when(scheduleLookupRepository.findById(jobId))
                .thenReturn(Optional.of(lookup));

        when(taskScheduleRepository.findById(key))
                .thenReturn(Optional.of(taskSchedule));

        jobService.deleteJob(userId, jobId);

        verify(taskScheduleRepository).deleteById(key);
        verify(scheduleLookupRepository).deleteById(jobId);
        verify(jobRepository).delete(job);
    }

    @Test
    void deleteJob_shouldThrowWhenJobDoesNotExist() {
        when(jobRepository.findByJobPrimaryKey(
                any(JobPrimaryKey.class)))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> jobService.deleteJob(userId, jobId)
        );

        verifyNoInteractions(scheduleLookupRepository);
        verifyNoInteractions(taskScheduleRepository);
    }

    @Test
    void deleteJob_shouldThrowWhenScheduleLookupDoesNotExist() {
        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .build();

        when(jobRepository.findByJobPrimaryKey(
                any(JobPrimaryKey.class)))
                .thenReturn(job);

        when(scheduleLookupRepository.findById(jobId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> jobService.deleteJob(userId, jobId)
        );

        verifyNoInteractions(taskScheduleRepository);
    }
}