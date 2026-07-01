package com.saurav.jobservice.service.impl;
import com.saurav.jobservice.dto.JobDto;
import com.saurav.jobservice.entity.Job;
import com.saurav.jobservice.entity.TaskSchedule;
import com.saurav.jobservice.exception.ResourceNotFoundException;
import com.saurav.jobservice.mapper.JobMapper;
import com.saurav.jobservice.mapper.TaskScheduleMapper;
import com.saurav.jobservice.repository.JobRepository;
import com.saurav.jobservice.repository.TaskScheduleRepository;
import com.saurav.jobservice.service.JobService;
import com.saurav.jobservice.util.JobDtoPrimaryKey;
import com.saurav.jobservice.util.JobPrimaryKey;
import com.saurav.jobservice.util.TaskSchedulePrimaryKey;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    /*
        * Method to create a new job for a user. It generates a unique job ID, sets the user ID,
        *  and saves the job to the database. Additionally, it calculates the next execution time
        * based on the job's interval and creates a corresponding task schedule entry.
     */
    @Override
    public JobDto createJob(String userId, JobDto jobDto) {

        JobDtoPrimaryKey primaryKey = new JobDtoPrimaryKey();
        primaryKey.setJobId(UUID.randomUUID());
        primaryKey.setUserId(UUID.fromString(userId));
        jobDto.setJobDtoPrimaryKey(primaryKey);
        jobDto.setCreatedTime(Instant.now());

        Job job = jobMapper.toEntity(jobDto);

        long nextExecutionTime = LocalDateTime.now()
                .plus(Duration.parse(job.getInterval()))
                .atZone(ZoneOffset.systemDefault())
                .toInstant()
                .getEpochSecond() / 60;

        int segment = calculateSegment(job.getJobPrimaryKey().getJobId());

        TaskSchedule taskSchedule = taskScheduleMapper.toTaskSchedule(
                job,
                nextExecutionTime,
                segment
        );

        taskScheduleRepository.save(taskSchedule);

        return jobMapper.toDto(jobRepository.save(job));
    }

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
}