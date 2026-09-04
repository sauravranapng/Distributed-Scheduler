package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.executor.factory.JobExecutorFactory;
import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.util.ExecutionContext;
import com.saurav.executorservice.service.ExecutionTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceImplTest {

    @Mock
    private ExecutionTrackingService executionTrackingService;

    @Mock
    private JobExecutorFactory jobExecutorFactory;

    @Mock
    private JobExecutor jobExecutor;

    @InjectMocks
    private JobExecutionServiceImpl jobExecutionService;

    @Test
    void execute_shouldStartTrackingAndCompleteSuccessfully() {
        JobExecutionEvent event = buildEvent(JobType.HTTP);
        ExecutionContext context = new ExecutionContext();

        when(executionTrackingService.startExecution(event, 3)).thenReturn(context);
        when(jobExecutorFactory.getExecutor(JobType.HTTP)).thenReturn(jobExecutor);

        jobExecutionService.execute(event, 3);

        verify(executionTrackingService).startExecution(event, 3);
        verify(jobExecutorFactory).getExecutor(JobType.HTTP);
        verify(jobExecutor).execute(event);
        verify(executionTrackingService).completeExecution(context);
    }

    @Test
    void execute_shouldIgnoreDuplicateExecutionAttempt() {
        JobExecutionEvent event = buildEvent(JobType.EMAIL);

        when(executionTrackingService.startExecution(event, 2)).thenReturn(null);

        jobExecutionService.execute(event, 2);

        verify(executionTrackingService).startExecution(event, 2);
        verifyNoInteractions(jobExecutorFactory);
        verifyNoInteractions(jobExecutor);
        verify(executionTrackingService, never()).completeExecution(any());
        verify(executionTrackingService, never()).failExecution(any(), any());
    }

    @Test
    void execute_shouldFailExecutionAndRethrowException() {
        JobExecutionEvent event = buildEvent(JobType.EMAIL);
        ExecutionContext context = new ExecutionContext();
        RuntimeException failure = new RuntimeException("executor crashed");

        when(executionTrackingService.startExecution(event, 1)).thenReturn(context);
        when(jobExecutorFactory.getExecutor(JobType.EMAIL)).thenReturn(jobExecutor);
        doThrow(failure).when(jobExecutor).execute(event);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> jobExecutionService.execute(event, 1));

        assertSame(failure, thrown);
        verify(executionTrackingService).failExecution(context, failure);
        verify(executionTrackingService, never()).completeExecution(any());
    }

    private JobExecutionEvent buildEvent(JobType jobType) {
        return JobExecutionEvent.builder()
                .executionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jobId(UUID.randomUUID())
                .scheduledExecutionTime(Instant.now())
                .jobType(jobType)
                .jobPayload("{\"sample\":true}")
                .build();
    }
}
