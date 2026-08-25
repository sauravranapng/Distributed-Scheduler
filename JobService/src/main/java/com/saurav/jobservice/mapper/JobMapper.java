package com.saurav.jobservice.mapper;


import com.saurav.jobservice.model.entity.Job;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.payload.EmailJobPayload;
import com.saurav.jobservice.model.payload.HttpJobPayload;
import com.saurav.jobservice.model.primarykey.JobPrimaryKey;
import com.saurav.jobservice.model.request.ScheduleRequest;
import com.saurav.jobservice.model.response.EmailJobDetailsResponse;
import com.saurav.jobservice.model.response.HttpJobDetailsResponse;
import com.saurav.jobservice.model.response.JobDetailsResponse;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.model.response.JobSummaryResponse;
import org.springframework.http.HttpMethod;
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
        job.setJobPayload(payload);
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
    public EmailJobDetailsResponse toEmailJobResponse(
            Job job,
            EmailJobPayload payload) {

        EmailJobDetailsResponse response = new EmailJobDetailsResponse();

        populateCommonFields(job, response);

        response.setTo(payload.getTo());
        response.setSubject(payload.getSubject());
        response.setBody(payload.getBody());

        return response;
    }


    public HttpJobDetailsResponse toHttpJobResponse(
            Job job,
            HttpJobPayload payload) {

        HttpJobDetailsResponse response = new HttpJobDetailsResponse();

        populateCommonFields(job, response);

        response.setMethod(HttpMethod.valueOf(payload.getMethod()));
        response.setUrl(payload.getUrl());
        response.setHeaders(payload.getHeaders());
        response.setBody(payload.getBody());
        response.setTimeoutSeconds(payload.getTimeoutSeconds());

        return response;
    }
    private void populateCommonFields(
            Job job,
            JobDetailsResponse response) {

        response.setUserId(job.getJobPrimaryKey().getUserId());
        response.setJobId(job.getJobPrimaryKey().getJobId());
        response.setJobType(job.getJobType());

        response.setRecurring(job.isRecurring());
        response.setInterval(job.getInterval());
        response.setMaxExecutions(job.getMaxExecutions());

        response.setStartTime(job.getStartTime());
        response.setEndTime(job.getEndTime());

        response.setCreatedTime(job.getCreatedTime());
    }

    public JobSummaryResponse toJobSummaryResponse(Job job) {

        JobSummaryResponse response = new JobSummaryResponse();

        response.setUserId(job.getJobPrimaryKey().getUserId());
        response.setJobId(job.getJobPrimaryKey().getJobId());

        response.setJobType(job.getJobType());

        response.setRecurring(job.isRecurring());
        response.setInterval(job.getInterval());

        response.setStartTime(job.getStartTime());
        response.setEndTime(job.getEndTime());

        response.setCreatedTime(job.getCreatedTime());

        return response;
    }
}
