package com.saurav.schedulingservice.service;

import com.saurav.schedulingservice.mapper.TaskScheduleMapper;
import com.saurav.schedulingservice.model.entity.ScheduleLookup;
import com.saurav.schedulingservice.model.entity.TaskSchedule;
import com.saurav.schedulingservice.model.enums.JobType;
import com.saurav.schedulingservice.model.event.AssignmentChangedEvent;
import com.saurav.schedulingservice.model.event.JobExecutionEvent;
import com.saurav.schedulingservice.model.primarykey.TaskSchedulePrimaryKey;
import com.saurav.schedulingservice.repository.ScheduleLookupRepository;
import com.saurav.schedulingservice.repository.TaskScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private TaskScheduleMapper taskScheduleMapper;

    @Mock
    private LeaderElectionService leaderElectionService;

    @Mock
    private TaskScheduleRepository taskScheduleRepository;

    @Mock
    private ScheduleLookupRepository scheduleLookupRepository;

    @Mock
    private KafkaTemplate<String, JobExecutionEvent> kafkaTemplate;

    private SchedulingService schedulingService;

    private UUID jobId;
    private UUID userId;

    @BeforeEach
    void setUp() {

        jobId = UUID.randomUUID();
        userId = UUID.randomUUID();

        when(leaderElectionService
                .getAssignedSegmentsForCurrentInstance())
                .thenReturn(List.of(1));

        schedulingService =
                new SchedulingService(
                        taskScheduleMapper,
                        leaderElectionService,
                        taskScheduleRepository,
                        scheduleLookupRepository,
                        kafkaTemplate
                );

        ReflectionTestUtils.setField(
                schedulingService,
                "kafkaTopic",
                "scheduling-topic"
        );
    }

    @Test
    void fetchAndPublishJobs_shouldDoNothingWhenNoSegmentsAssigned() {

        when(leaderElectionService
                .getAssignedSegmentsForCurrentInstance())
                .thenReturn(List.of());

        schedulingService.fetchAndPublishJobs();

        verifyNoInteractions(taskScheduleRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void fetchAndPublishJobs_shouldDoNothingWhenAssignedSegmentsAreNull() {

        when(leaderElectionService
                .getAssignedSegmentsForCurrentInstance())
                .thenReturn(null);

        schedulingService.fetchAndPublishJobs();

        verifyNoInteractions(taskScheduleRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void fetchAndPublishJobs_shouldDoNothingWhenNoJobsAreDue() {

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of());

        schedulingService.fetchAndPublishJobs();

        verify(taskScheduleRepository)
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void fetchAndPublishJobs_shouldProcessJobsForAssignedSegments() {

        TaskSchedule task = createOneTimeTask(100L);

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(task));

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        schedulingService.fetchAndPublishJobs();

        verify(taskScheduleRepository)
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                );

        verify(kafkaTemplate)
                .send(
                        anyString(),
                        eq(jobId.toString()),
                        any(JobExecutionEvent.class)
                );
    }

    @Test
    void fetchAndPublishJobs_shouldFetchJobsFromEveryAssignedSegment() {

        when(leaderElectionService
                .getAssignedSegmentsForCurrentInstance())
                .thenReturn(List.of(1, 2, 3));

        when(taskScheduleRepository
                .findJobsForCurrentMinute(anyLong(), anyInt()))
                .thenReturn(List.of());

        schedulingService.fetchAndPublishJobs();

        verify(taskScheduleRepository)
                .findJobsForCurrentMinute(anyLong(), eq(1));

        verify(taskScheduleRepository)
                .findJobsForCurrentMinute(anyLong(), eq(2));

        verify(taskScheduleRepository)
                .findJobsForCurrentMinute(anyLong(), eq(3));
    }

    @Test
    void fetchAndPublishJobs_shouldPublishExecutionEventWithCorrectData()
            throws Exception {

        long scheduledMinute = Instant.now()
                .getEpochSecond() / 60;

        TaskSchedule task =
                createOneTimeTask(scheduledMinute);

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(task));

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        schedulingService.fetchAndPublishJobs();

        ArgumentCaptor<JobExecutionEvent> captor =
                ArgumentCaptor.forClass(JobExecutionEvent.class);

        verify(kafkaTemplate).send(
                anyString(),
                eq(jobId.toString()),
                captor.capture()
        );

        JobExecutionEvent event =
                captor.getValue();

        assertEquals(jobId, event.getJobId());
        assertEquals(userId, event.getUserId());
        assertEquals(JobType.HTTP, event.getJobType());
        assertEquals(
                "{\"url\":\"https://example.com\"}",
                event.getJobPayload()
        );

        assertEquals(
                Instant.ofEpochSecond(
                        scheduledMinute * 60
                ),
                event.getScheduledExecutionTime()
        );

        assertNotNull(event.getExecutionId());
    }

    @Test
    void fetchAndPublishJobs_shouldPublishMultipleJobs() {

        TaskSchedule first =
                createOneTimeTask(100L);

        TaskSchedule second =
                createOneTimeTask(100L);

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(first, second));

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        schedulingService.fetchAndPublishJobs();

        verify(kafkaTemplate, times(2))
                .send(
                        anyString(),
                        anyString(),
                        any(JobExecutionEvent.class)
                );
    }

    @Test
    void fetchAndPublishJobs_shouldDeleteOneTimeJobAfterSuccessfulPublish() throws InterruptedException {

        TaskSchedule task =
                createOneTimeTask(100L);

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(task));

        SendResult<String, JobExecutionEvent> sendResult = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        schedulingService.fetchAndPublishJobs();
        
        Thread.sleep(100);

        verify(taskScheduleRepository)
                .delete(task);

        verify(scheduleLookupRepository)
                .deleteById(jobId);
    }

    @Test
    void fetchAndPublishJobs_shouldNotDeleteJobWhenKafkaPublishFails() {

        TaskSchedule task =
                createOneTimeTask(100L);

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(task));

        CompletableFuture<SendResult<String, JobExecutionEvent>>
                failedFuture = new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException("Kafka unavailable")
        );

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(failedFuture);

        schedulingService.fetchAndPublishJobs();

        verify(taskScheduleRepository, never())
                .delete(task);

        verify(scheduleLookupRepository, never())
                .deleteById(jobId);
    }

    @Test
    void recurringJob_shouldBeRescheduledAfterSuccessfulPublish() throws InterruptedException {

        TaskSchedule task =
                createRecurringTask(
                        100L,
                        "PT5M",
                        3
                );

        TaskSchedule nextSchedule =
                createRecurringTask(
                        105L,
                        "PT5M",
                        2
                );

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(task));

        when(taskScheduleMapper.copyWithNextExecutionTime(
                eq(task),
                eq(105L),
                eq(2)
        )).thenReturn(nextSchedule);

        SendResult<String, JobExecutionEvent> sendResult = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        schedulingService.fetchAndPublishJobs();
        
        Thread.sleep(100);

        verify(taskScheduleRepository)
                .save(nextSchedule);

        verify(scheduleLookupRepository)
                .save(any(ScheduleLookup.class));

        verify(taskScheduleRepository)
                .delete(task);
    }

    @Test
    void recurringJob_shouldDeleteAfterFinalExecution() throws InterruptedException {

        TaskSchedule task =
                createRecurringTask(
                        100L,
                        "PT5M",
                        1
                );

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(task));

        SendResult<String, JobExecutionEvent> sendResult = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        schedulingService.fetchAndPublishJobs();
        
        Thread.sleep(100);

        verify(taskScheduleRepository)
                .delete(task);

        verify(scheduleLookupRepository)
                .deleteById(jobId);

        verify(taskScheduleMapper, never())
                .copyWithNextExecutionTime(
                        any(),
                        anyLong(),
                        any()
                );
    }

    @Test
    void assignmentChanged_shouldTriggerCatchUpProcessing() {

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of());

        schedulingService.onAssignmentChanged(
                new AssignmentChangedEvent()
        );

        verify(
                taskScheduleRepository,
                atLeast(2)
        ).findJobsForCurrentMinute(
                anyLong(),
                eq(1)
        );
    }

    private TaskSchedule createOneTimeTask(
            long executionMinute
    ) {

        return TaskSchedule.builder()
                .key(
                        new TaskSchedulePrimaryKey(
                                executionMinute,
                                1,
                                jobId
                        )
                )
                .userId(userId)
                .jobType(JobType.HTTP)
                .jobPayload(
                        "{\"url\":\"https://example.com\"}"
                )
                .recurring(false)
                .build();
    }

    private TaskSchedule createRecurringTask(
            long executionMinute,
            String interval,
            Integer remainingExecutions
    ) {

        return TaskSchedule.builder()
                .key(
                        new TaskSchedulePrimaryKey(
                                executionMinute,
                                1,
                                jobId
                        )
                )
                .userId(userId)
                .jobType(JobType.HTTP)
                .jobPayload(
                        "{\"url\":\"https://example.com\"}"
                )
                .recurring(true)
                .interval(interval)
                .remainingExecutions(remainingExecutions)
                .build();
    }

    @Test
    void recurringJob_shouldDeleteWhenNextExecutionExceedsEndTime() throws InterruptedException {

        long currentExecution = 100L;

        TaskSchedule task =
                TaskSchedule.builder()
                        .key(new TaskSchedulePrimaryKey(
                                currentExecution,
                                1,
                                jobId
                        ))
                        .userId(userId)
                        .jobType(JobType.HTTP)
                        .jobPayload("{}")
                        .recurring(true)
                        .interval("PT5M")
                        .remainingExecutions(null)
                        .endTime(
                                Instant.ofEpochSecond(
                                        102L * 60L
                                )
                        )
                        .build();

        when(taskScheduleRepository
                .findJobsForCurrentMinute(
                        anyLong(),
                        eq(1)
                ))
                .thenReturn(List.of(task));

        SendResult<String, JobExecutionEvent> sendResult = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any(JobExecutionEvent.class)
        )).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        schedulingService.fetchAndPublishJobs();
        
        Thread.sleep(100);

        verify(taskScheduleRepository)
                .delete(task);

        verify(scheduleLookupRepository)
                .deleteById(jobId);

        verify(taskScheduleMapper, never())
                .copyWithNextExecutionTime(
                        any(),
                        anyLong(),
                        any()
                );
    }
}