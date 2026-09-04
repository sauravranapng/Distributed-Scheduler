package com.saurav.jobservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.response.JobResponse;
import com.saurav.jobservice.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @Test
    void createHttpJob_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobResponse response = JobResponse.builder()
                .userId(userId)
                .jobId(jobId)
                .jobType(JobType.HTTP)
                .build();

        when(jobService.createHttpJob(
                eq(userId),
                any()
        )).thenReturn(response);

        String request = """
                {
                  "method": "GET",
                  "url": "https://example.com",
                  "recurring": false
                }
                """;

        mockMvc.perform(
                        post("/jobservice/users/{userId}/jobs/http", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.jobType").value("HTTP"));
    }
}