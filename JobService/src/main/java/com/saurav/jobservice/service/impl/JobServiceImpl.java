package com.saurav.jobservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.jobservice.exception.PayloadDeserializationException;
import com.saurav.jobservice.exception.PayloadSerializationException;
import com.saurav.jobservice.exception.ResourceNotFoundException;
import com.saurav.jobservice.mapper.JobMapper;
import com.saurav.jobservice.mapper.ScheduleLookupMapper;
import com.saurav.jobservice.mapper.TaskScheduleMapper;
import com.saurav.jobservice.model.dto.JobDto;
import com.saurav.jobservice.model.entity.Job;
import com.saurav.jobservice.model.entity.ScheduleLookup;
import com.saurav.jobservice.model.entity.TaskSchedule;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.payload.EmailJobPayload;
import com.saurav.jobservice.model.payload.HttpJobPayload;
import com.saurav.jobservice.model.payload.JobPayload;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import com.saurav.jobservice.model.primarykey.TaskSchedulePrimaryKey;
import com.saurav.jobservice.model.request.CreateEmailJobRequest;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.request.ScheduleRequest;
import com.saurav.jobservice.model.request.UpdateEmailJobRequest;
import com.saurav.jobservice.model.request.UpdateHttpJobRequest;
import com.saurav.jobservice.model.response.JobDetailsResponse;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.model.response.JobSummaryResponse;
import com.saurav.jobservice.model.response.UpdateJobRequest;
import com.saurav.jobservice.repository.JobRepository;
import com.saurav.jobservice.repository.ScheduleLookupRepository;
import com.saurav.jobservice.repository.TaskScheduleRepository;
import com.saurav.jobservice.service.JobService;
import com.saurav.jobservice.util.JobServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.saurav.jobservice.util.JobServiceUtil.calculateNextExecutionTime;
import static com.saurav.jobservice.util.JobServiceUtil.calculateSegment;

@Service
@Slf4j
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;
    private final TaskScheduleMapper taskScheduleMapper;
    private final JobRepository jobRepository;
    private final TaskScheduleRepository taskScheduleRepository;
    private final ObjectMapper objectMapper;
    private final ScheduleLookupMapper scheduleLookupMapper;
    private final ScheduleLookupRepository scheduleLookupRepository;

    public JobServiceImpl(JobMapper jobMapper, TaskScheduleMapper taskScheduleMapper, JobRepository jobRepository , TaskScheduleRepository taskScheduleRepository, ObjectMapper objectMapper, ScheduleLookupMapper scheduleLookupMapper, ScheduleLookupRepository scheduleLookupRepository) {
        this.jobMapper = jobMapper;
        this.taskScheduleMapper = taskScheduleMapper;
        this.jobRepository = jobRepository;
        this.taskScheduleRepository=taskScheduleRepository;
        this.objectMapper = objectMapper;
        this.scheduleLookupMapper = scheduleLookupMapper;
        this.scheduleLookupRepository = scheduleLookupRepository;
    }

    @Override
    public JobResponse createHttpJob(
            UUID userId,
            CreateHttpJobRequest request) {

        HttpJobPayload payload = HttpJobPayload.builder()
                .method(request.getMethod())
                .url(request.getUrl())
                .headers(request.getHeaders())
                .body(request.getBody())
                .timeoutSeconds(request.getTimeoutSeconds())
                .build();

        return createJob(
                userId,
                JobType.HTTP,
                payload,
                request
        );
    }

    @Override
    public JobResponse createEmailJob(
            UUID userId,
            CreateEmailJobRequest request) {

        EmailJobPayload payload = EmailJobPayload.builder()
                .to(request.getTo())
                .subject(request.getSubject())
                .body(request.getBody())
                .build();

        return createJob(
                userId,
                JobType.EMAIL,
                payload,
                request
        );
    }

    // Method to retrieve a job by user ID and job ID
    @Override
    public JobDetailsResponse getJob(
            UUID userId,
            UUID jobId) {

        Job job = jobRepository.findByJobPrimaryKey(new JobPrimaryKey(userId, jobId));

        if (job == null) {throw new ResourceNotFoundException("Job", "userId", "jobId", userId.toString(), jobId.toString());}

        try {

            switch (job.getJobType()) {

                case HTTP -> {
                    HttpJobPayload payload =
                            objectMapper.readValue(job.getJobPayload(), HttpJobPayload.class);
                    return jobMapper.toHttpJobResponse(job, payload);
                }

                case EMAIL -> {
                    EmailJobPayload payload =
                            objectMapper.readValue(job.getJobPayload(), EmailJobPayload.class);
                    return jobMapper.toEmailJobResponse(job, payload);
                }

                default -> throw new IllegalStateException("Unsupported job type : " + job.getJobType());
            }

        } catch (JsonProcessingException ex) {

            throw new PayloadDeserializationException("Unable to deserialize payload", ex);
        }
    }

    // Method to update an existing job (you can add more logic to update specific fields)
    @Override
    public JobDetailsResponse updateJob(
            UUID userId,
            UUID jobId,
            UpdateJobRequest request) {

        JobPrimaryKey primaryKey = new JobPrimaryKey(userId, jobId);

        Job existingJob = jobRepository.findByJobPrimaryKey(primaryKey);

        if (existingJob == null) {
            throw new ResourceNotFoundException(
                    "Job",
                    "userId",
                    "jobId",
                    userId,
                    jobId);
        }

        ScheduleLookup existingLookup = scheduleLookupRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Scheduled Job",
                        "jobId",
                        jobId));

        TaskSchedulePrimaryKey oldPrimaryKey = new TaskSchedulePrimaryKey(
                existingLookup.getNextExecutionTime(),
                existingLookup.getSegment(),
                jobId);

        TaskSchedule oldTaskSchedule = taskScheduleRepository.findById(oldPrimaryKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task Schedule",
                        "jobId",
                        jobId));

        // Keep a copy for rollback
        Job oldJob = Job.builder()
                .jobPrimaryKey(existingJob.getJobPrimaryKey())
                .jobType(existingJob.getJobType())
                .jobPayload(existingJob.getJobPayload())
                .recurring(existingJob.isRecurring())
                .interval(existingJob.getInterval())
                .maxExecutions(existingJob.getMaxExecutions())
                .startTime(existingJob.getStartTime())
                .endTime(existingJob.getEndTime())
                .createdTime(existingJob.getCreatedTime())
                .updatedTime(existingJob.getUpdatedTime())
                .build();

        String payloadJson = JobServiceUtil.serializePayload(request);

        existingJob.setJobPayload(payloadJson);
        existingJob.setRecurring(request.isRecurring());
        existingJob.setInterval(request.getInterval());
        existingJob.setMaxExecutions(request.getMaxExecutions());
        existingJob.setStartTime(request.getStartTime());
        existingJob.setEndTime(request.getEndTime());
        existingJob.setUpdatedTime(Instant.now());

        long nextExecutionTime = calculateNextExecutionTime(Instant.now(), request);
        int segment = calculateSegment(jobId);

        TaskSchedule newTaskSchedule = taskScheduleMapper.toTaskSchedule(
                existingJob,
                nextExecutionTime,
                segment);

        ScheduleLookup newLookup = ScheduleLookup.builder()
                .jobId(jobId)
                .nextExecutionTime(nextExecutionTime)
                .segment(segment)
                .build();

        try {

            jobRepository.save(existingJob);
            taskScheduleRepository.save(newTaskSchedule);
            scheduleLookupRepository.save(newLookup);

            // Remove old schedule only after the new one exists
            taskScheduleRepository.deleteById(oldPrimaryKey);

            return getJob(userId, jobId);

        } catch (Exception ex) {

            rollbackJobUpdate(oldJob, oldTaskSchedule, existingLookup, newTaskSchedule, newLookup);

            throw ex;
        }
    }

    private void rollbackJobUpdate(
            Job oldJob,
            TaskSchedule oldTaskSchedule,
            ScheduleLookup oldLookup,
            TaskSchedule newTaskSchedule,
            ScheduleLookup newLookup) {

        try {
            jobRepository.save(oldJob);
        } catch (Exception rollbackEx) {
            log.error("Failed to restore job {}",
                    oldJob.getJobPrimaryKey().getJobId(), rollbackEx);
        }

        try {
            taskScheduleRepository.delete(newTaskSchedule);
        } catch (Exception rollbackEx) {
            log.error("Failed to remove new task schedule {}",
                    newTaskSchedule.getKey().getJobId(), rollbackEx);
        }

        try {
            scheduleLookupRepository.deleteById(newLookup.getJobId());
        } catch (Exception rollbackEx) {
            log.error("Failed to remove new schedule lookup {}",
                    newLookup.getJobId(), rollbackEx);
        }

        try {
            taskScheduleRepository.save(oldTaskSchedule);
        } catch (Exception rollbackEx) {
            log.error("Failed to restore old task schedule {}",
                    oldTaskSchedule.getKey().getJobId(), rollbackEx);
        }

        try {
            scheduleLookupRepository.save(oldLookup);
        } catch (Exception rollbackEx) {
            log.error("Failed to restore old schedule lookup {}",
                    oldLookup.getJobId(), rollbackEx);
        }
    }

    @Override
    public void deleteJob(UUID userId, UUID jobId) {

        JobPrimaryKey primaryKey = new JobPrimaryKey(userId, jobId);

        Job job = jobRepository.findByJobPrimaryKey(primaryKey);

        if (job == null) {
            throw new ResourceNotFoundException(
                    "Job",
                    "userId",
                    "jobId",
                    userId,
                    jobId);
        }

        ScheduleLookup scheduleLookup = scheduleLookupRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job Schedule",
                        "jobId",
                        jobId));

        TaskSchedulePrimaryKey taskPrimaryKey = new TaskSchedulePrimaryKey(
                scheduleLookup.getNextExecutionTime(),
                scheduleLookup.getSegment(),
                jobId);

        TaskSchedule taskSchedule = taskScheduleRepository.findById(taskPrimaryKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task Schedule",
                        "jobId",
                        jobId));

        try {

            // Stop future executions first
            taskScheduleRepository.deleteById(taskPrimaryKey);

            // Remove lookup metadata
            scheduleLookupRepository.deleteById(jobId);

            // Remove job configuration
            jobRepository.delete(job);

        } catch (Exception ex) {

            rollbackJobDeletion(taskSchedule, scheduleLookup);

            throw ex;
        }
    }

    private void rollbackJobDeletion(
            TaskSchedule taskSchedule,
            ScheduleLookup scheduleLookup) {

        try {
            taskScheduleRepository.save(taskSchedule);
        } catch (Exception rollbackEx) {
            log.error(
                    "Failed to restore task_schedule for jobId={}",
                    taskSchedule.getKey().getJobId(),
                    rollbackEx);
        }

        try {
            scheduleLookupRepository.save(scheduleLookup);
        } catch (Exception rollbackEx) {
            log.error(
                    "Failed to restore schedule_lookup for jobId={}",
                    scheduleLookup.getJobId(),
                    rollbackEx);
        }
    }
    @Override
    public List<JobSummaryResponse> getJobsByUser(
            UUID userId) {

        return jobRepository
                .findByJobPrimaryKeyUserId(userId)
                .stream()
                .map(jobMapper::toJobSummaryResponse)
                .toList();
    }

    private JobResponse createJob(
            UUID userId,
            JobType jobType,
            JobPayload payload,
            ScheduleRequest request) {

        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();

        String payloadJson = JobServiceUtil.serializePayload(payload);

        JobPrimaryKey primaryKey = new JobPrimaryKey(userId, jobId);

        Job job = jobMapper.toEntity(
                primaryKey,
                jobType,
                payloadJson,
                request,
                now
        );

        long nextExecutionTime = calculateNextExecutionTime(now, request);

        int segment = calculateSegment(jobId);

        TaskSchedule taskSchedule = taskScheduleMapper.toTaskSchedule(
                job,
                nextExecutionTime,
                segment
        );

        ScheduleLookup scheduleLookup =
                scheduleLookupMapper.toEntity(
                        jobId,
                        nextExecutionTime,
                        segment
                );

        jobRepository.save(job);

        try {
            taskScheduleRepository.save(taskSchedule);
            scheduleLookupRepository.save(scheduleLookup);

        } catch (Exception ex) {

            rollbackJobCreation(job, taskSchedule);

            throw ex;
        }
        return jobMapper.toResponse(job);
    }

    private void rollbackJobCreation(Job job, TaskSchedule taskSchedule) {

        try {
            taskScheduleRepository.delete(taskSchedule);
        } catch (Exception ex) {
            log.error("Failed to rollback task schedule. jobId={}",
                    job.getJobPrimaryKey().getJobId(), ex);
        }

        try {
            jobRepository.deleteByJobPrimaryKey(job.getJobPrimaryKey());
        } catch (Exception ex) {
            log.error("Failed to rollback job. jobId={}",
                    job.getJobPrimaryKey().getJobId(), ex);
        }
    }
}