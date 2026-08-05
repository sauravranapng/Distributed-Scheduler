package com.saurav.jobservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.jobservice.exception.PayloadDeserializationException;
import com.saurav.jobservice.exception.PayloadSerializationException;
import com.saurav.jobservice.exception.ResourceNotFoundException;
import com.saurav.jobservice.mapper.JobMapper;
import com.saurav.jobservice.mapper.TaskScheduleMapper;
import com.saurav.jobservice.model.dto.JobDto;
import com.saurav.jobservice.model.entity.Job;
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
import com.saurav.jobservice.repository.TaskScheduleRepository;
import com.saurav.jobservice.service.JobService;
import com.saurav.jobservice.util.JobServiceUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.saurav.jobservice.util.JobServiceUtil.calculateNextExecutionTime;
import static com.saurav.jobservice.util.JobServiceUtil.calculateSegment;

@Service
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;
    private final TaskScheduleMapper taskScheduleMapper;
    private final JobRepository jobRepository;
    private final TaskScheduleRepository taskScheduleRepository;
    private final ObjectMapper objectMapper;


    public JobServiceImpl(JobMapper jobMapper, TaskScheduleMapper taskScheduleMapper, JobRepository jobRepository , TaskScheduleRepository taskScheduleRepository, ObjectMapper objectMapper) {
        this.jobMapper = jobMapper;
        this.taskScheduleMapper = taskScheduleMapper;
        this.jobRepository = jobRepository;
        this.taskScheduleRepository=taskScheduleRepository;
        this.objectMapper = objectMapper;
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
                    userId.toString(),
                    jobId.toString());
        }

        String payloadJson;

        try {

            JobType jobType = existingJob.getJobType();

            if (jobType == JobType.HTTP) {

                UpdateHttpJobRequest httpRequest =
                        (UpdateHttpJobRequest) request;

                HttpJobPayload payload = HttpJobPayload.builder()
                        .method(httpRequest.getMethod())
                        .url(httpRequest.getUrl())
                        .headers(httpRequest.getHeaders())
                        .body(httpRequest.getBody())
                        .timeoutSeconds(httpRequest.getTimeoutSeconds())
                        .build();

                payloadJson = objectMapper.writeValueAsString(payload);

            } else {

                UpdateEmailJobRequest emailRequest =
                        (UpdateEmailJobRequest) request;

                EmailJobPayload payload = EmailJobPayload.builder()
                        .to(emailRequest.getTo())
                        .subject(emailRequest.getSubject())
                        .body(emailRequest.getBody())
                        .build();

                payloadJson = objectMapper.writeValueAsString(payload);
            }

        } catch (JsonProcessingException ex) {

            throw new PayloadSerializationException(
                    "Failed to serialize payload",
                    ex);
        }

        existingJob.setJobPayload(payloadJson);
        existingJob.setRecurring(request.isRecurring());
        existingJob.setInterval(request.getInterval());
        existingJob.setMaxExecutions(request.getMaxExecutions());
        existingJob.setStartTime(request.getStartTime());
        existingJob.setEndTime(request.getEndTime());
        existingJob.setUpdatedTime(Instant.now());

        jobRepository.save(existingJob);

        // Update TaskSchedule
        long nextExecutionTime =
                calculateNextExecutionTime(
                        Instant.now(),
                        request);

        TaskSchedule taskSchedule =
                taskScheduleMapper.toTaskSchedule(
                        existingJob,
                        nextExecutionTime,
                        calculateSegment(jobId));

        taskScheduleRepository.save(taskSchedule);

        return getJob(userId, jobId);
    }

    @Override
    public void deleteJob(
            UUID userId,
            UUID jobId) {

        JobPrimaryKey primaryKey =
                new JobPrimaryKey(userId, jobId);

        Job job = jobRepository.findByJobPrimaryKey(primaryKey);

        if (job == null) {
            throw new ResourceNotFoundException("Job", "userId", "jobId", userId.toString(), jobId.toString());
        }

        jobRepository.delete(job);

        TaskSchedulePrimaryKey taskPrimaryKey =
                new TaskSchedulePrimaryKey(
                        calculateNextExecutionTime(job.getStartTime(), job),
                        calculateSegment(jobId),
                        jobId);

        taskScheduleRepository.deleteById(taskPrimaryKey);
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

        JobPrimaryKey primaryKey = new JobPrimaryKey(
                userId,
                jobId
        );

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

        jobRepository.save(job);

        try {

            taskScheduleRepository.save(taskSchedule);

        } catch (Exception ex) {

            jobRepository.deleteByJobPrimaryKey(job.getJobPrimaryKey());

            throw ex;
        }
        return jobMapper.toResponse(job);
    }
}