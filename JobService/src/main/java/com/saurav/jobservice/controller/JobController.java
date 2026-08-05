package com.saurav.jobservice.controller;

import com.saurav.jobservice.model.request.CreateEmailJobRequest;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.response.JobDetailsResponse;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.model.response.JobSummaryResponse;
import com.saurav.jobservice.model.response.UpdateJobRequest;
import com.saurav.jobservice.service.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobservice/users")
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/{userId}/jobs/http")
    public ResponseEntity<JobResponse> createHttpJob(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateHttpJobRequest request) {

        return ResponseEntity.ok(
                jobService.createHttpJob(userId, request)
        );
    }

    @PostMapping("/{userId}/jobs/email")
    public ResponseEntity<JobResponse> createEmailJob(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateEmailJobRequest request) {

        return ResponseEntity.ok(
                jobService.createEmailJob(userId, request)
        );
    }

    @GetMapping("/{user_id}/jobs/{job_id}")
    public ResponseEntity<JobDetailsResponse> getJob(
            @PathVariable("user_id") UUID userId,
            @PathVariable("job_id") UUID jobId) {

        return ResponseEntity.ok(
                jobService.getJob(userId, jobId)
        );
    }

    @PutMapping("/{user_id}/jobs/{job_id}")
    public ResponseEntity<JobDetailsResponse> updateJob(
            @PathVariable("user_id") UUID userId,
            @PathVariable("job_id") UUID jobId,
            @RequestBody UpdateJobRequest request) {

        return ResponseEntity.ok(
                jobService.updateJob(userId, jobId, request)
        );
    }

    @DeleteMapping("/{user_id}/jobs/{job_id}")
    public ResponseEntity<Void> deleteJob(
            @PathVariable("user_id") UUID userId,
            @PathVariable("job_id") UUID jobId) {

        jobService.deleteJob(userId, jobId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{user_id}/jobs")
    public ResponseEntity<List<JobSummaryResponse>> getJobsByUser(
            @PathVariable("user_id") UUID userId) {

        return ResponseEntity.ok(
                jobService.getJobsByUser(userId)
        );
    }



}
