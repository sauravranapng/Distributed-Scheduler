package com.saurav.jobservice.model.request;

import jakarta.validation.constraints.AssertTrue;
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
     * "2026-08-10T10:00:00Z"
     */
    private Instant startTime;

    /**
     * Whether the job should execute repeatedly.
     */
    private boolean recurring;

    /**
     * ISO-8601 Duration.
     * Examples:
     * PT30S
     * PT5M
     * PT1H
     * P1D
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
    private Integer maxExecutions;

    /**
     * Stop scheduling after this time.
     * Null means no end time.
     */
    private Instant endTime;

    /**
     * Validation:
     * Interval should exist for recurring jobs.
     */
    @AssertTrue(message = "Recurring jobs must specify an interval.")
    public boolean isIntervalValid() {
        return !recurring || interval != null;
    }

    /**
     * Validation:
     * At least one scheduling termination strategy should exist
     * for recurring jobs.
     */
    @AssertTrue(message = "Recurring jobs must specify maxExecutions or endTime.")
    public boolean isValidRecurringConfiguration() {

        if (!recurring) {
            return true;
        }

        return maxExecutions != null || endTime != null;
    }
}