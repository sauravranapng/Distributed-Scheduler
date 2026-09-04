package com.saurav.jobservice.model.request;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleRequestTest {

    @Test
    void intervalValid_shouldReturnTrueForNonRecurringJob() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(false);
        request.setInterval(null);

        assertTrue(request.isIntervalValid());
    }

    @Test
    void intervalValid_shouldReturnTrueWhenRecurringJobHasInterval() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(true);
        request.setInterval("PT5M");

        assertTrue(request.isIntervalValid());
    }

    @Test
    void intervalValid_shouldReturnFalseWhenRecurringJobHasNoInterval() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(true);
        request.setInterval(null);

        assertFalse(request.isIntervalValid());
    }

    @Test
    void recurringConfiguration_shouldRequireMaxExecutionsOrEndTime() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(true);
        request.setInterval("PT5M");

        assertFalse(request.isValidRecurringConfiguration());

        request.setMaxExecutions(5);

        assertTrue(request.isValidRecurringConfiguration());
    }

    @Test
    void recurringConfiguration_shouldAllowEndTimeInsteadOfMaxExecutions() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(true);
        request.setInterval("PT5M");
        request.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));

        assertTrue(request.isValidRecurringConfiguration());
    }

    @Test
    void endTimeAfterStartTime_shouldReturnTrueForValidDates() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);

        request.setStartTime(start);
        request.setEndTime(end);

        assertTrue(request.isEndTimeAfterStartTime());
    }

    @Test
    void endTimeAfterStartTime_shouldReturnFalseForInvalidDates() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        Instant start = Instant.now();
        Instant end = start.minus(1, ChronoUnit.HOURS);

        request.setStartTime(start);
        request.setEndTime(end);

        assertFalse(request.isEndTimeAfterStartTime());
    }

    @Test
    void intervalAllowedForOneTimeJob_shouldRejectInterval() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(false);
        request.setInterval("PT5M");

        assertFalse(request.isIntervalAllowedForOneTimeJobs());
    }

    @Test
    void maxExecutionsAllowedForOneTimeJob_shouldRejectMaxExecutions() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(false);
        request.setMaxExecutions(5);

        assertFalse(request.isMaxExecutionsAllowedForOneTimeJobs());
    }

    @Test
    void endTimeAllowedForOneTimeJob_shouldRejectEndTime() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(false);
        request.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));

        assertFalse(request.isEndTimeAllowedForOneTimeJobs());
    }

    @Test
    void minimumInterval_shouldAcceptOneMinute() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(true);
        request.setInterval("PT1M");

        assertTrue(request.isMinimumIntervalValid());
    }

    @Test
    void minimumInterval_shouldRejectLessThanOneMinute() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(true);
        request.setInterval("PT30S");

        assertFalse(request.isMinimumIntervalValid());
    }

    @Test
    void minimumInterval_shouldRejectInvalidDuration() {
        CreateHttpJobRequest request = new CreateHttpJobRequest();

        request.setRecurring(true);
        request.setInterval("invalid");

        assertFalse(request.isMinimumIntervalValid());
    }
}