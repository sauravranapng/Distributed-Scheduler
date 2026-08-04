package com.saurav.jobservice.mapper;


import com.saurav.jobservice.model.entity.Job;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import com.saurav.jobservice.model.request.ScheduleRequest;
import com.saurav.jobservice.model.response.JobResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JobMapper {

    public Job toEntity(
            JobPrimaryKey primaryKey,
            JobType jobType,
            String payload,
            ScheduleRequest request,
            Instant createdTime) {

        Job job = new Job();

        job.setJobPrimaryKey(primaryKey);
        job.setJobType(jobType);
        job.setPayload(payload);

        job.setRecurring(request.isRecurring());
        job.setInterval(request.getInterval());
        job.setMaxExecutions(request.getMaxExecutions());
        job.setEndTime(request.getEndTime());

        job.setCreatedTime(createdTime);

        return job;
    }

    public JobResponse toResponse(Job job) {

        JobResponse response = new JobResponse();

        response.setUserId(job.getJobPrimaryKey().getUserId());
        response.setJobId(job.getJobPrimaryKey().getJobId());

        response.setJobType(job.getJobType());

        response.setRecurring(job.isRecurring());
        response.setInterval(job.getInterval());

        response.setMaxExecutions(job.getMaxExecutions());
        response.setEndTime(job.getEndTime());

        response.setCreatedTime(job.getCreatedTime());

        return response;
    }
}
