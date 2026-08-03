package com.saurav.jobservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.saurav.jobservice.model.primarykey.JobDtoPrimaryKey;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import com.saurav.jobservice.model.primarykey.TaskSchedulePrimaryKey;
import com.saurav.jobservice.model.request.CreateEmailJobRequest;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.request.ScheduleRequest;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.repository.JobRepository;
import com.saurav.jobservice.repository.TaskScheduleRepository;
import com.saurav.jobservice.service.JobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.saurav.jobservice.util.JobServiceUtil.calculateSegment;

@Service
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;
    private final TaskScheduleMapper taskScheduleMapper;
    private final JobRepository jobRepository;
    private final TaskScheduleRepository taskScheduleRepository;

    public JobServiceImpl(JobMapper jobMapper, TaskScheduleMapper taskScheduleMapper, JobRepository jobRepository , TaskScheduleRepository taskScheduleRepository) {
        this.jobMapper = jobMapper;
        this.taskScheduleMapper = taskScheduleMapper;
        this.jobRepository = jobRepository;
        this.taskScheduleRepository=taskScheduleRepository;
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

   /* @Override
    public JobDto createJob(String userId, JobDto jobDto) {

        JobDtoPrimaryKey primaryKey = new JobDtoPrimaryKey();
        primaryKey.setJobId(UUID.randomUUID());
        primaryKey.setUserId(UUID.fromString(userId));
        jobDto.setJobDtoPrimaryKey(primaryKey);
        jobDto.setCreatedTime(Instant.now());

        Job job = jobMapper.toEntity(jobDto);

        long nextExecutionTime = Instant.now()
                .plus(Duration.parse(job.getInterval()))
                .getEpochSecond() / 60;

        int segment = calculateSegment(job.getJobPrimaryKey().getJobId());

        TaskSchedule taskSchedule = taskScheduleMapper.toTaskSchedule(
                job,
                nextExecutionTime,
                segment
        );

        taskScheduleRepository.save(taskSchedule);

        return jobMapper.toDto(jobRepository.save(job));
    }*/

    // Method to retrieve a job by user ID and job ID
    @Override
    public JobDto getJob(String userId, String jobId) {
        Job job = jobRepository.findByJobPrimaryKey(new JobPrimaryKey(UUID.fromString(userId), UUID.fromString(jobId)));
        if (job == null) {
            throw new ResourceNotFoundException("Job","userId","jobId",userId,jobId);
        }
        return jobMapper.toDto(job);
    }
    // Method to update an existing job (you can add more logic to update specific fields)
    @Override
    public JobDto updateJob( String userId ,String jobId,JobDto jobDto) {
        jobDto.getJobDtoPrimaryKey().setJobId(UUID.fromString(jobId));
        jobDto.getJobDtoPrimaryKey().setUserId(UUID.fromString(userId));
        jobDto.setCreatedTime(Instant.now());
        Job job = jobMapper.toEntity(jobDto);
        return  jobMapper.toDto(jobRepository.save(job));
    }

    @Override
    public void deleteJob(String  userId ,String jobId) {
        jobRepository.deleteByJobPrimaryKey(new JobPrimaryKey(UUID.fromString(userId), UUID.fromString(jobId)));
    }

    @Override
    public List<JobDto> getJobsByUser(UUID userId) {

        List<Job> jobs = jobRepository.findByJobPrimaryKeyUserId(userId);

        return jobs.stream()
                .map(jobMapper::toDto)
                .toList();
    }

    @Transactional
    private JobResponse createJob(
            UUID userId,
            JobType jobType,
            JobPayload payload,
            ScheduleRequest request) {

        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();

        String payloadJson;

        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new PayloadSerializationException(
                    "Failed to serialize job payload.",
                    ex);
        }

        Job job = Job.builder()
                .jobPrimaryKey(new JobPrimaryKey(userId, jobId))
                .jobType(jobType)
                .payload(payloadJson)
                .recurring(request.isRecurring())
                .interval(request.getInterval())
                .maxExecutions(request.getMaxExecutions())
                .endTime(request.getEndTime())
                .createdTime(now)
                .build();

        long nextExecutionTime = request.isRecurring()
                ? now.plus(Duration.parse(request.getInterval()))
                .getEpochSecond() / 60
                : now.getEpochSecond() / 60;

        int segment = calculateSegment(jobId);

        TaskSchedule taskSchedule = TaskSchedule.builder()
                .key(new TaskSchedulePrimaryKey(
                        nextExecutionTime,
                        segment,
                        jobId))
                .userId(userId)
                .jobType(jobType)
                .payload(payloadJson)
                .recurring(request.isRecurring())
                .interval(request.getInterval())
                .remainingExecutions(request.getMaxExecutions())
                .endTime(request.getEndTime())
                .build();

        jobRepository.save(job);
        taskScheduleRepository.save(taskSchedule);

        return JobResponse.builder()
                .jobId(jobId)
                .recurring(job.isRecurring())
                .jobType(jobType)
                .createdTime(now)
                .build();
    }
}