package com.saurav.jobservice.service;

import com.saurav.jobservice.dto.JobDto;

import java.util.List;
import java.util.UUID;


public interface JobService {
    JobDto createJob(String userId, JobDto jobDto);
    JobDto getJob(String userId, String jobId);
    JobDto updateJob( String userId ,String jobId,JobDto jobDto);
    void deleteJob(String  userId ,String jobId);
    List<JobDto> getJobsByUser(UUID userId);
}

