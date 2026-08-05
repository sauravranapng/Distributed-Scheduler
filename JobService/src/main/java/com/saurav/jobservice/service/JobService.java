package com.saurav.jobservice.service;


import com.saurav.jobservice.model.dto.JobDto;
import com.saurav.jobservice.model.request.CreateEmailJobRequest;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.response.JobDetailsResponse;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.model.response.JobSummaryResponse;
import com.saurav.jobservice.model.response.UpdateJobRequest;

import java.util.List;
import java.util.UUID;


public interface JobService {
    JobResponse createHttpJob(UUID userId, CreateHttpJobRequest request);
    JobResponse createEmailJob(UUID userId, CreateEmailJobRequest request);
    JobDetailsResponse getJob(
            UUID userId,
            UUID jobId);

    JobDetailsResponse updateJob(
            UUID userId,
            UUID jobId,
            UpdateJobRequest request);

    void deleteJob(
            UUID userId,
            UUID jobId);

    List<JobSummaryResponse> getJobsByUser(
            UUID userId);
}

