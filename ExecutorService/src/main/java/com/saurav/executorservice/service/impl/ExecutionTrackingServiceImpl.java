package com.saurav.executorservice.service.impl;

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
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionTrackingServiceImpl implements ExecutionTrackingService {

    private static final int INITIAL_ATTEMPT = 1;

    private final ExecutionHistoryRepository historyRepository;

    private final ExecutionAttemptRepository attemptRepository;

    @Override
    public ExecutionContext startExecution(JobExecutionEvent event , int attemptNumber) {

        Instant now = Instant.now();

        ExecutionHistory history = ExecutionHistory.builder()
                .executionId(event.getExecutionId())
                .jobId(event.getJobId())
                .userId(event.getUserId())
                .scheduledExecutionTime(event.getScheduledExecutionTime())
                .executionStatus(ExecutionStatus.RUNNING)
                .startedAt(now)
                .lastAttemptTime(now)
                .totalAttempts(attemptNumber)
                .build();

        ExecutionAttempt attempt = ExecutionAttempt.builder()
                .primaryKey(new ExecutionAttemptPrimaryKey(
                        event.getExecutionId(),
                        attemptNumber))
                .startedAt(now)
                .executionStatus(ExecutionStatus.RUNNING)
                .build();

        ExecutionContext context = ExecutionContext.builder()
                .executionHistory(history)
                .executionAttempt(attempt)
                .build();

        persist(context);

        return context;
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