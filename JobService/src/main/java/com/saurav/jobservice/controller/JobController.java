package com.saurav.jobservice.controller;
import com.saurav.jobservice.dto.JobDto;
import com.saurav.jobservice.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
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

    @PostMapping("/{user_id}/jobs")
    public ResponseEntity<JobDto> createJob(@PathVariable("user_id") String userId, @RequestBody JobDto jobDto) {
        return ResponseEntity.status(HttpStatus.OK).body(jobService.createJob(userId,jobDto));
    }

    @GetMapping("/{user_id}/jobs/{job_id}")
    public ResponseEntity<JobDto> getJob(@PathVariable("user_id") String userId, @PathVariable("job_id") String jobId) {
        return ResponseEntity.status(HttpStatus.OK).body(jobService.getJob(userId,jobId));
    }

    @PutMapping("/{user_id}/jobs/{job_id}")
    public ResponseEntity<JobDto> updateJob(
            @PathVariable("user_id") String userId,
            @PathVariable("job_id") String jobId,
            @RequestBody JobDto jobDto) {
        JobDto updatedJob = jobService.updateJob(userId, jobId, jobDto);
        return ResponseEntity.status(HttpStatus.OK).body(updatedJob);
    }

    @DeleteMapping("/{user_id}/jobs/{job_id}")
    public ResponseEntity<Void> deleteJob(@PathVariable("user_id") String userId, @PathVariable("job_id") String jobId) {
        jobService.deleteJob(userId, jobId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{user_id}/jobs")
    public ResponseEntity<List<JobDto>> getJobsByUser(
            @PathVariable("user_id") UUID userId) {

        return ResponseEntity.ok(jobService.getJobsByUser(userId));
    }



}
