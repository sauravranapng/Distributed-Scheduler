package com.saurav.jobservice.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class ScheduleRequest {

    /**
     * When the first execution should happen.
     * Null = execute immediately.
     */
    private Instant startTime;

    /**
     * Whether the job should execute repeatedly.
     */
    private boolean recurring;

    /**
     * ISO-8601 Duration.
     */
    @Pattern(
            regexp = "^P.*",
            message = "Interval must be a valid ISO-8601 Duration (e.g. PT5M, PT1H)."
    )
    private String interval;

    /**
     * Maximum number of executions.
     * Null means unlimited executions.
     */
    @Min(value = 1, message = "maxExecutions must be at least 1.")
    private Integer maxExecutions;

    /**
     * Stop scheduling after this time.
     */
    private Instant endTime;

    /**
     * Recurring jobs must specify an interval.
     */
    @AssertTrue(message = "Recurring jobs must specify an interval.")
    public boolean isIntervalValid() {
        return !recurring || interval != null;
    }

    /**
     * Recurring jobs must specify either maxExecutions or endTime.
     */
    @AssertTrue(message = "Recurring jobs must specify maxExecutions or endTime.")
    public boolean isValidRecurringConfiguration() {
        return !recurring || maxExecutions != null || endTime != null;
    }

    /**
     * endTime must be after startTime.
     */
    @AssertTrue(message = "endTime must be after startTime.")
    public boolean isEndTimeAfterStartTime() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    /**
     * Non-recurring jobs should not specify an interval.
     */
    @AssertTrue(message = "Non-recurring jobs should not specify an interval.")
    public boolean isIntervalAllowedForOneTimeJobs() {
        return recurring || interval == null;
    }

    /**
     * Non-recurring jobs should not specify maxExecutions.
     */
    @AssertTrue(message = "Non-recurring jobs should not specify maxExecutions.")
    public boolean isMaxExecutionsAllowedForOneTimeJobs() {
        return recurring || maxExecutions == null;
    }

    /**
     * Non-recurring jobs should not specify endTime.
     */
    @AssertTrue(message = "Non-recurring jobs should not specify endTime.")
    public boolean isEndTimeAllowedForOneTimeJobs() {
        return recurring || endTime == null;
    }
}