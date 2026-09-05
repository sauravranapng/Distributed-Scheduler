package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.exception.RetryableExecutionException;
import com.saurav.executorservice.model.entity.ExecutionAttempt;
import com.saurav.executorservice.model.entity.ExecutionHistory;
import com.saurav.executorservice.model.enums.ExecutionStatus;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.primarykey.ExecutionAttemptPrimaryKey;
import com.saurav.executorservice.model.util.ExecutionContext;
import com.saurav.executorservice.repository.ExecutionAttemptRepository;
import com.saurav.executorservice.repository.ExecutionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.EntityWriteResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionTrackingServiceImplTest {

    @Mock
    private ExecutionHistoryRepository historyRepository;

    @Mock
    private ExecutionAttemptRepository attemptRepository;

    @Mock
    private CassandraTemplate cassandraTemplate;

    @Mock
    private EntityWriteResult<ExecutionAttempt> writeResult;

    private ExecutionTrackingServiceImpl service;

    @BeforeEach
    void setUp() {

        service =
                new ExecutionTrackingServiceImpl(
                        historyRepository,
                        attemptRepository,
                        cassandraTemplate);
    }

    @Test
    void startExecution_shouldCreateNewAttemptAndHistory() {

        JobExecutionEvent event = buildEvent();

        when(attemptRepository.findById(any()))
                .thenReturn(Optional.empty());

        when(cassandraTemplate.insert(
                any(ExecutionAttempt.class),
                any()))
                .thenReturn(writeResult);

        when(writeResult.wasApplied())
                .thenReturn(true);

        ExecutionContext context =
                service.startExecution(event, 1);

        assertNotNull(context);

        assertNotNull(context.getExecutionAttempt());
        assertNotNull(context.getExecutionHistory());

        assertEquals(
                ExecutionStatus.RUNNING,
                context.getExecutionAttempt()
                        .getExecutionStatus());

        assertEquals(
                ExecutionStatus.RUNNING,
                context.getExecutionHistory()
                        .getExecutionStatus());

        assertEquals(
                1,
                context.getExecutionHistory()
                        .getTotalAttempts());

        verify(historyRepository)
                .save(any(ExecutionHistory.class));
    }

    @Test
    void startExecution_shouldReturnNullWhenLwtInsertWasNotApplied() {

        JobExecutionEvent event = buildEvent();

        when(attemptRepository.findById(any()))
                .thenReturn(Optional.empty());

        when(cassandraTemplate.insert(
                any(ExecutionAttempt.class),
                any()))
                .thenReturn(writeResult);

        when(writeResult.wasApplied())
                .thenReturn(false);

        ExecutionContext context =
                service.startExecution(event, 1);

        assertNull(context);
    }

    @Test
    void startExecution_shouldIgnoreCompletedAttempt() {

        JobExecutionEvent event = buildEvent();

        ExecutionAttempt attempt =
                buildAttempt(
                        ExecutionStatus.COMPLETED,
                        Instant.now().plusSeconds(60));

        when(attemptRepository.findById(any()))
                .thenReturn(Optional.of(attempt));

        ExecutionContext context =
                service.startExecution(event, 1);

        assertNull(context);
    }

    @Test
    void startExecution_shouldIgnoreFailedAttempt() {

        JobExecutionEvent event = buildEvent();

        ExecutionAttempt attempt =
                buildAttempt(
                        ExecutionStatus.FAILED,
                        Instant.now().plusSeconds(60));

        when(attemptRepository.findById(any()))
                .thenReturn(Optional.of(attempt));

        ExecutionContext context =
                service.startExecution(event, 1);

        assertNull(context);
    }

    @Test
    void startExecution_shouldThrowRetryableExceptionWhenLeaseIsActive() {

        JobExecutionEvent event = buildEvent();

        ExecutionAttempt attempt =
                buildAttempt(
                        ExecutionStatus.RUNNING,
                        Instant.now().plusSeconds(60));

        when(attemptRepository.findById(any()))
                .thenReturn(Optional.of(attempt));

        assertThrows(
                RetryableExecutionException.class,
                () -> service.startExecution(event, 1));
    }

    @Test
    void startExecution_shouldReclaimExpiredLease() {

        JobExecutionEvent event = buildEvent();

        Instant expiredLease =
                Instant.now().minusSeconds(60);

        ExecutionAttempt attempt =
                buildAttempt(
                        ExecutionStatus.RUNNING,
                        expiredLease);

        ExecutionHistory history =
                ExecutionHistory.builder()
                        .executionId(event.getExecutionId())
                        .jobId(event.getJobId())
                        .userId(event.getUserId())
                        .executionStatus(ExecutionStatus.FAILED)
                        .build();

        when(attemptRepository.findById(any()))
                .thenReturn(Optional.of(attempt));

        when(historyRepository.findById(event.getExecutionId()))
                .thenReturn(Optional.of(history));

        ExecutionContext context =
                service.startExecution(event, 1);

        assertNotNull(context);

        assertEquals(
                ExecutionStatus.RUNNING,
                context.getExecutionAttempt()
                        .getExecutionStatus());

        assertEquals(
                ExecutionStatus.RUNNING,
                context.getExecutionHistory()
                        .getExecutionStatus());

        assertNotNull(
                context.getExecutionAttempt()
                        .getLeaseUntil());

        verify(attemptRepository)
                .save(attempt);

        verify(historyRepository)
                .save(history);
    }

    @Test
    void completeExecution_shouldMarkAttemptAndHistoryCompleted() {

        Instant startedAt =
                Instant.now().minusSeconds(10);

        ExecutionHistory history =
                ExecutionHistory.builder()
                        .executionId(UUID.randomUUID())
                        .executionStatus(ExecutionStatus.RUNNING)
                        .build();

        ExecutionAttempt attempt =
                ExecutionAttempt.builder()
                        .primaryKey(
                                new ExecutionAttemptPrimaryKey(
                                        history.getExecutionId(),
                                        1))
                        .startedAt(startedAt)
                        .executionStatus(ExecutionStatus.RUNNING)
                        .build();

        ExecutionContext context =
                ExecutionContext.builder()
                        .executionHistory(history)
                        .executionAttempt(attempt)
                        .build();

        service.completeExecution(context);

        assertEquals(
                ExecutionStatus.COMPLETED,
                history.getExecutionStatus());

        assertEquals(
                ExecutionStatus.COMPLETED,
                attempt.getExecutionStatus());

        assertNotNull(history.getCompletedAt());
        assertNotNull(attempt.getCompletedAt());
        assertNotNull(attempt.getDurationMs());

        verify(historyRepository).save(history);
        verify(attemptRepository).save(attempt);
    }

    @Test
    void failExecution_shouldMarkAttemptAndHistoryFailed() {

        Instant startedAt =
                Instant.now().minusSeconds(10);

        ExecutionHistory history =
                ExecutionHistory.builder()
                        .executionId(UUID.randomUUID())
                        .executionStatus(ExecutionStatus.RUNNING)
                        .build();

        ExecutionAttempt attempt =
                ExecutionAttempt.builder()
                        .primaryKey(
                                new ExecutionAttemptPrimaryKey(
                                        history.getExecutionId(),
                                        1))
                        .startedAt(startedAt)
                        .executionStatus(ExecutionStatus.RUNNING)
                        .build();

        ExecutionContext context =
                ExecutionContext.builder()
                        .executionHistory(history)
                        .executionAttempt(attempt)
                        .build();

        RuntimeException failure =
                new RuntimeException("HTTP request failed");

        service.failExecution(context, failure);

        assertEquals(
                ExecutionStatus.FAILED,
                history.getExecutionStatus());

        assertEquals(
                ExecutionStatus.FAILED,
                attempt.getExecutionStatus());

        assertEquals(
                "HTTP request failed",
                history.getErrorMessage());

        assertEquals(
                "HTTP request failed",
                attempt.getErrorMessage());

        assertNotNull(history.getCompletedAt());
        assertNotNull(attempt.getCompletedAt());
        assertNotNull(attempt.getDurationMs());

        verify(historyRepository).save(history);
        verify(attemptRepository).save(attempt);
    }

    private ExecutionAttempt buildAttempt(
            ExecutionStatus status,
            Instant leaseUntil) {

        return ExecutionAttempt.builder()
                .primaryKey(
                        new ExecutionAttemptPrimaryKey(
                                UUID.randomUUID(),
                                1))
                .startedAt(Instant.now())
                .executionStatus(status)
                .leaseUntil(leaseUntil)
                .build();
    }

    private JobExecutionEvent buildEvent() {

        return JobExecutionEvent.builder()
                .executionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jobId(UUID.randomUUID())
                .scheduledExecutionTime(Instant.now())
                .jobType(
                        com.saurav.executorservice.model.enums.JobType.HTTP)
                .jobPayload("{}")
                .build();
    }
}