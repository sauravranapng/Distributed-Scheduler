package com.saurav.jobservice.util;

import com.saurav.jobservice.model.payload.EmailJobPayload;
import com.saurav.jobservice.model.payload.HttpJobPayload;
import com.saurav.jobservice.model.request.CreateHttpJobRequest;
import com.saurav.jobservice.model.request.UpdateEmailJobRequest;
import com.saurav.jobservice.model.request.UpdateHttpJobRequest;
import com.saurav.jobservice.model.response.UpdateJobRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobServiceUtilTest {

    @Test
    void calculateSegment_shouldReturnValueWithinValidRange() {
        UUID jobId = UUID.randomUUID();

        int segment = JobServiceUtil.calculateSegment(jobId);

        assertTrue(segment >= 0);
        assertTrue(segment < Constants.TOTAL_SEGMENTS);
    }

    @Test
    void calculateSegment_shouldBeDeterministic() {
        UUID jobId = UUID.randomUUID();

        int first = JobServiceUtil.calculateSegment(jobId);
        int second = JobServiceUtil.calculateSegment(jobId);

        assertEquals(first, second);
    }

    @Test
    void serializePayload_shouldSerializeHttpPayload() {
        HttpJobPayload payload = HttpJobPayload.builder()
                .method("GET")
                .url("https://example.com")
                .headers(Map.of("Authorization", "Bearer test"))
                .body(null)
                .timeoutSeconds(10)
                .build();

        String result = JobServiceUtil.serializePayload(payload);

        assertNotNull(result);
        assertTrue(result.contains("\"method\":\"GET\""));
        assertTrue(result.contains("\"url\":\"https://example.com\""));
        assertTrue(result.contains("\"timeoutSeconds\":10"));
    }

    @Test
    void serializePayload_shouldSerializeEmailPayload() {
        EmailJobPayload payload = EmailJobPayload.builder()
                .to(List.of("test@example.com"))
                .subject("Test")
                .body("Hello")
                .build();

        String result = JobServiceUtil.serializePayload(payload);

        assertNotNull(result);
        assertTrue(result.contains("test@example.com"));
        assertTrue(result.contains("\"subject\":\"Test\""));
        assertTrue(result.contains("\"body\":\"Hello\""));
    }

    @Test
    void serializePayload_shouldSerializeUpdateHttpRequest() {
        UpdateHttpJobRequest request = new UpdateHttpJobRequest();
        request.setMethod(HttpMethod.POST);
        request.setUrl("https://example.com/api");
        request.setHeaders(Map.of("Content-Type", "application/json"));
        request.setBody("{\"name\":\"test\"}");
        request.setTimeoutSeconds(30);

        String result = JobServiceUtil.serializePayload(request);

        assertTrue(result.contains("\"method\":\"POST\""));
        assertTrue(result.contains("\"url\":\"https://example.com/api\""));
        assertTrue(result.contains("\"timeoutSeconds\":30"));
    }

    @Test
    void serializePayload_shouldSerializeUpdateEmailRequest() {
        UpdateEmailJobRequest request = new UpdateEmailJobRequest(
                List.of("user@example.com"),
                "Subject",
                "Email body"
        );

        String result = JobServiceUtil.serializePayload(request);

        assertTrue(result.contains("user@example.com"));
        assertTrue(result.contains("\"subject\":\"Subject\""));
        assertTrue(result.contains("\"body\":\"Email body\""));
    }

    @Test
    void serializePayload_shouldRejectUnsupportedUpdateRequest() {
        UpdateJobRequest request = new UpdateJobRequest() {
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JobServiceUtil.serializePayload(request)
        );

        assertTrue(exception.getMessage().contains("Unsupported UpdateJobRequest type"));
    }

    @Test
    void calculateNextExecutionTime_shouldUseNowWhenStartTimeIsNull() {
        Instant now = Instant.parse("2026-09-06T10:00:00Z");

        CreateHttpJobRequest request = new CreateHttpJobRequest();
        request.setRecurring(false);

        long result = JobServiceUtil.calculateNextExecutionTime(now, request);

        assertEquals(now.getEpochSecond() / 60, result);
    }

    @Test
    void calculateNextExecutionTime_shouldUseFutureStartTimeForOneTimeJob() {
        Instant now = Instant.parse("2026-09-06T10:00:00Z");
        Instant start = Instant.parse("2026-09-06T11:00:00Z");

        CreateHttpJobRequest request = new CreateHttpJobRequest();
        request.setStartTime(start);
        request.setRecurring(false);

        long result = JobServiceUtil.calculateNextExecutionTime(now, request);

        assertEquals(start.getEpochSecond() / 60, result);
    }

    @Test
    void calculateNextExecutionTime_shouldReturnFutureStartForRecurringJob() {
        Instant now = Instant.parse("2026-09-06T10:00:00Z");
        Instant start = Instant.parse("2026-09-06T11:00:00Z");

        CreateHttpJobRequest request = new CreateHttpJobRequest();
        request.setStartTime(start);
        request.setRecurring(true);
        request.setInterval("PT5M");

        long result = JobServiceUtil.calculateNextExecutionTime(now, request);

        assertEquals(start.getEpochSecond() / 60, result);
    }

    @Test
    void calculateNextExecutionTime_shouldCalculateNextIntervalForPastRecurringStart() {
        Instant now = Instant.parse("2026-09-06T10:10:00Z");
        Instant start = Instant.parse("2026-09-06T10:00:00Z");

        CreateHttpJobRequest request = new CreateHttpJobRequest();
        request.setStartTime(start);
        request.setRecurring(true);
        request.setInterval("PT5M");

        long result = JobServiceUtil.calculateNextExecutionTime(now, request);

        Instant expected = Instant.parse("2026-09-06T10:10:00Z");

        assertEquals(expected.getEpochSecond() / 60, result);
    }

    @Test
    void calculateNextExecutionTime_shouldRoundUpToNextInterval() {
        Instant now = Instant.parse("2026-09-06T10:11:00Z");
        Instant start = Instant.parse("2026-09-06T10:00:00Z");

        CreateHttpJobRequest request = new CreateHttpJobRequest();
        request.setStartTime(start);
        request.setRecurring(true);
        request.setInterval("PT5M");

        long result = JobServiceUtil.calculateNextExecutionTime(now, request);

        Instant expected = Instant.parse("2026-09-06T10:15:00Z");

        assertEquals(expected.getEpochSecond() / 60, result);
    }
}