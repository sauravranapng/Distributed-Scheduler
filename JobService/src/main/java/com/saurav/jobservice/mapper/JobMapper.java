package com.saurav.jobservice.mapper;

import com.saurav.jobservice.dto.JobDto;
import com.saurav.jobservice.entity.Job;
import com.saurav.jobservice.util.JobDtoPrimaryKey;
import com.saurav.jobservice.util.JobPrimaryKey;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobDto toDto(Job job) {

        JobDto dto = new JobDto();

        dto.setJobDtoPrimaryKey(
                new JobDtoPrimaryKey(
                        job.getJobPrimaryKey().getUserId(),
                        job.getJobPrimaryKey().getJobId()));

        dto.setDescription(job.getDescription());
        dto.setInterval(job.getInterval());
        dto.setRecurring(job.isRecurring());
        dto.setMaxRetryCount(job.getMaxRetryCount());
        dto.setCreatedTime(job.getCreatedTime());

        return dto;
    }

    public Job toEntity(JobDto dto) {

        Job job = new Job();

        job.setJobPrimaryKey(
                new JobPrimaryKey(
                        dto.getJobDtoPrimaryKey().getUserId(),
                        dto.getJobDtoPrimaryKey().getJobId()));

        job.setDescription(dto.getDescription());
        job.setInterval(dto.getInterval());
        job.setRecurring(dto.isRecurring());
        job.setMaxRetryCount(dto.getMaxRetryCount());
        job.setCreatedTime(dto.getCreatedTime());

        return job;
    }
}
