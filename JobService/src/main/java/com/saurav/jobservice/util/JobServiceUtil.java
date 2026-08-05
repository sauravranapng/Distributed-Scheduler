package com.saurav.jobservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saurav.jobservice.exception.PayloadSerializationException;
import com.saurav.jobservice.model.payload.JobPayload;
import com.saurav.jobservice.model.request.ScheduleRequest;

import java.time.Instant;
import java.util.UUID;

import static com.saurav.jobservice.util.Constants.TOTAL_SEGMENTS;

public class JobServiceUtil {
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
