package com.saurav.jobservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.jobservice.exception.PayloadSerializationException;
import com.saurav.jobservice.model.enums.JobType;
import com.saurav.jobservice.model.payload.EmailJobPayload;
import com.saurav.jobservice.model.payload.HttpJobPayload;
import com.saurav.jobservice.model.payload.JobPayload;
import com.saurav.jobservice.model.request.ScheduleRequest;
import com.saurav.jobservice.model.request.UpdateEmailJobRequest;
import com.saurav.jobservice.model.request.UpdateHttpJobRequest;
import com.saurav.jobservice.model.response.UpdateJobRequest;

import java.time.Instant;
import java.util.UUID;

import static com.saurav.jobservice.util.Constants.TOTAL_SEGMENTS;

public class JobServiceUtil {
    static final ObjectMapper objectMapper = new ObjectMapper();
    private JobServiceUtil(){

    }
    /*
     This method calculates the segment number for a given jobId.
     */
    public static int calculateSegment(UUID jobId) {
        return Math.floorMod(jobId.hashCode(), TOTAL_SEGMENTS);
    }

    public static String serializePayload(JobPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new PayloadSerializationException(
                    "Failed to serialize job payload.",
                    ex);
        }
    }
    public static String serializePayload(UpdateJobRequest request) {

        if (request instanceof UpdateHttpJobRequest httpRequest) {

            HttpJobPayload payload = HttpJobPayload.builder()
                    .method(httpRequest.getMethod())
                    .url(httpRequest.getUrl())
                    .headers(httpRequest.getHeaders())
                    .body(httpRequest.getBody())
                    .timeoutSeconds(httpRequest.getTimeoutSeconds())
                    .build();

            return serializePayload(payload);
        }

        if (request instanceof UpdateEmailJobRequest emailRequest) {

            EmailJobPayload payload = EmailJobPayload.builder()
                    .to(emailRequest.getTo())
                    .subject(emailRequest.getSubject())
                    .body(emailRequest.getBody())
                    .build();

            return serializePayload(payload);
        }

        throw new IllegalArgumentException(
                "Unsupported UpdateJobRequest type: " + request.getClass().getSimpleName());
    }


    public static long calculateNextExecutionTime(
            Instant now,
            ScheduleRequest request) {

        Instant firstExecutionTime = request.getStartTime();

        if (firstExecutionTime == null) {
            firstExecutionTime = now;
        }

        return firstExecutionTime.getEpochSecond() / 60;
    }
}
