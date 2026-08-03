package com.saurav.jobservice.service;


import com.saurav.jobservice.model.dto.JobDto;
import com.saurav.jobservice.model.request.CreateEmailJobRequest;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.response.JobResponse;

import java.util.List;
import java.util.UUID;


public interface JobService {
    JobResponse createHttpJob(UUID userId, CreateHttpJobRequest request);
    JobResponse createEmailJob(UUID userId, CreateEmailJobRequest request);
    JobDto getJob(String userId, String jobId);
    JobDto updateJob( String userId ,String jobId,JobDto jobDto);
    void deleteJob(String  userId ,String jobId);
    List<JobDto> getJobsByUser(UUID userId);
}

