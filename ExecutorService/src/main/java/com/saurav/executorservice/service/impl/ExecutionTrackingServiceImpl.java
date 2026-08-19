package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.exception.RetryableExecutionException;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.entity.ExecutionAttempt;
import com.saurav.executorservice.model.primarykey.ExecutionAttemptPrimaryKey;
import com.saurav.executorservice.model.entity.ExecutionHistory;
import com.saurav.executorservice.model.enums.ExecutionStatus;
import com.saurav.executorservice.model.util.ExecutionContext;
import com.saurav.executorservice.repository.ExecutionAttemptRepository;
import com.saurav.executorservice.repository.ExecutionHistoryRepository;
import com.saurav.executorservice.service.ExecutionTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.EntityWriteResult;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionTrackingServiceImpl implements ExecutionTrackingService {

    private static final int INITIAL_ATTEMPT = 1;

    private static final Duration EXECUTION_LEASE = Duration.ofMinutes(2);

    private final ExecutionHistoryRepository historyRepository;

    private final ExecutionAttemptRepository attemptRepository;

    private final CassandraTemplate cassandraTemplate;

    @Override
    public ExecutionContext startExecution(
            JobExecutionEvent event,
            int attemptNumber) {

        Instant now = Instant.now();

        ExecutionAttemptPrimaryKey primaryKey =
                new ExecutionAttemptPrimaryKey(
                        event.getExecutionId(),
                        attemptNumber);

        // Check whether this execution attempt already exists
        Optional<ExecutionAttempt> existingAttempt =
                attemptRepository.findById(primaryKey);

        /*
         * First delivery of this attempt.
         */
        if (existingAttempt.isEmpty()) {

            Instant leaseUntil = now.plus(EXECUTION_LEASE);

            ExecutionAttempt attempt = ExecutionAttempt.builder()
                    .primaryKey(primaryKey)
                    .startedAt(now)
                    .executionStatus(ExecutionStatus.RUNNING)
                    .leaseUntil(leaseUntil)
                    .build();

            EntityWriteResult<ExecutionAttempt> result =
                    cassandraTemplate.insert(
                            attempt,
                            InsertOptions.builder()
                                    .withIfNotExists()
                                    .build());

            /*
             * Another executor may have inserted the same attempt
             * between findById() and INSERT IF NOT EXISTS.
             */
            if (!result.wasApplied()) {
                return null;
            }

            ExecutionHistory history = ExecutionHistory.builder()
                    .executionId(event.getExecutionId())
                    .jobId(event.getJobId())
                    .userId(event.getUserId())
                    .scheduledExecutionTime(
                            event.getScheduledExecutionTime()
                                    .getEpochSecond() / 60)
                    .executionStatus(ExecutionStatus.RUNNING)
                    .startedAt(now)
                    .lastAttemptTime(now)
                    .totalAttempts(attemptNumber)
                    .build();

            historyRepository.save(history);

            return ExecutionContext.builder()
                    .executionHistory(history)
                    .executionAttempt(attempt)
                    .build();
        }

        /*
         * Attempt already exists.
         */
        ExecutionAttempt attempt = existingAttempt.get();

        /*
         * The execution has already completed.
         */
        if (attempt.getExecutionStatus() == ExecutionStatus.COMPLETED) {

            log.info(
                    "Execution already completed. executionId={}, attempt={}",
                    event.getExecutionId(),
                    attemptNumber);

            return null;
        }

        /*
         * This particular delivery attempt already failed.
         *
         * A new Kafka retry will have a different attemptNumber.
         */
        if (attempt.getExecutionStatus() == ExecutionStatus.FAILED) {

            log.info("Execution attempt already failed. executionId={}, attempt={}",
                    event.getExecutionId(),
                    attemptNumber);

            return null;
        }

        /*
         * Previous executor is still within its lease.
         */
        if (attempt.getExecutionStatus() == ExecutionStatus.RUNNING
                && attempt.getLeaseUntil() != null
                && attempt.getLeaseUntil().isAfter(now)) {

            log.info("Execution attempt is still active. executionId={}, attempt={}",
                    event.getExecutionId(),
                    attemptNumber);

            throw new RetryableExecutionException(
                    "Execution attempt is still active. executionId="
                            + event.getExecutionId()
                            + ", attempt="
                            + attemptNumber,
                    null);        }

        /*
         * RUNNING + expired lease.
         *
         * The previous executor is considered crashed/stuck.
         * Reclaim the execution atomically using Cassandra LWT.
         */
        if (attempt.getExecutionStatus() == ExecutionStatus.RUNNING
                && (attempt.getLeaseUntil() == null
                || !attempt.getLeaseUntil().isAfter(now))) {

            return reclaimExecution(
                    event,
                    attempt,
                    now);
        }

        return null;
    }

    private ExecutionContext reclaimExecution(
            JobExecutionEvent event,
            ExecutionAttempt attempt,
            Instant now) {

        Instant newLeaseUntil = now.plus(EXECUTION_LEASE);

        attempt.setStartedAt(now);
        attempt.setLeaseUntil(newLeaseUntil);

        ExecutionHistory history =
                historyRepository.findById(event.getExecutionId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Execution history not found for executionId="
                                                + event.getExecutionId()));

        history.setExecutionStatus(ExecutionStatus.RUNNING);
        history.setLastAttemptTime(now);

        attemptRepository.save(attempt);
        historyRepository.save(history);

        log.info(
                "Reclaimed expired execution. executionId={}, attempt={}",
                event.getExecutionId(),
                attempt.getPrimaryKey().getAttemptNumber());

        return ExecutionContext.builder()
                .executionHistory(history)
                .executionAttempt(attempt)
                .build();
    }

    @Override
    public void completeExecution(ExecutionContext context) {

        Instant now = Instant.now();

        ExecutionHistory history = context.getExecutionHistory();
        history.setExecutionStatus(ExecutionStatus.COMPLETED);
        history.setCompletedAt(now);
        history.setLastAttemptTime(now);

        ExecutionAttempt attempt = context.getExecutionAttempt();
        attempt.setExecutionStatus(ExecutionStatus.COMPLETED);
        attempt.setCompletedAt(now);
        attempt.setDurationMs(
                now.toEpochMilli()
                        - attempt.getStartedAt().toEpochMilli());

        persist(context);
    }

    @Override
    public void failExecution(
            ExecutionContext context,
            Exception exception) {

        Instant now = Instant.now();

        String error = exception == null
                ? null
                : exception.getMessage();

        ExecutionHistory history = context.getExecutionHistory();
        history.setExecutionStatus(ExecutionStatus.FAILED);
        history.setCompletedAt(now);
        history.setLastAttemptTime(now);
        history.setErrorMessage(error);

        ExecutionAttempt attempt = context.getExecutionAttempt();
        attempt.setExecutionStatus(ExecutionStatus.FAILED);
        attempt.setCompletedAt(now);
        attempt.setErrorMessage(error);
        attempt.setDurationMs(
                now.toEpochMilli()
                        - attempt.getStartedAt().toEpochMilli());

        persist(context);
    }

    private void persist(ExecutionContext context) {

        ExecutionHistory history = context.getExecutionHistory();
        ExecutionAttempt attempt = context.getExecutionAttempt();

        try {

            historyRepository.save(history);
            attemptRepository.save(attempt);

        } catch (Exception ex) {

            log.error(
                    "Failed to persist execution history. executionId={}, attempt={}",
                    history.getExecutionId(),
                    attempt.getPrimaryKey().getAttemptNumber(),
                    ex);

            throw ex;
        }
    }
}