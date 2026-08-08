package com.saurav.schedulingservice.service;
import com.saurav.schedulingservice.mapper.TaskScheduleMapper;
import com.saurav.schedulingservice.model.entity.ScheduleLookup;
import com.saurav.schedulingservice.model.entity.TaskSchedule;
import com.saurav.schedulingservice.model.event.AssignmentChangedEvent;
import com.saurav.schedulingservice.model.event.JobExecutionEvent;
import com.saurav.schedulingservice.repository.ScheduleLookupRepository;
import com.saurav.schedulingservice.repository.TaskScheduleRepository;
import com.saurav.schedulingservice.util.ExecutionIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SchedulingService {
    private final TaskScheduleMapper taskScheduleMapper;
    private final LeaderElectionService leaderElectionService;
    private final TaskScheduleRepository taskScheduleRepository;
    private final ScheduleLookupRepository scheduleLookupRepository;
    private final KafkaTemplate<String, JobExecutionEvent> kafkaTemplate;
    private List<Integer> assignedSegments;
    @Value("${app.kafka.topic}")
    private String kafkaTopic;
    private static final Logger logger = LoggerFactory.getLogger(SchedulingService.class);

    @Autowired
    public SchedulingService(TaskScheduleMapper taskScheduleMapper, LeaderElectionService leaderElectionService,
                             TaskScheduleRepository taskScheduleRepository, ScheduleLookupRepository scheduleLookupRepository, KafkaTemplate<String, JobExecutionEvent> kafkaTemplate) {
        this.taskScheduleMapper = taskScheduleMapper;
        this.leaderElectionService = leaderElectionService;
        this.taskScheduleRepository = taskScheduleRepository;
        this.scheduleLookupRepository = scheduleLookupRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.assignedSegments = leaderElectionService.getAssignedSegmentsForCurrentInstance();
    }

    /**
     * Retrieves all jobs scheduled for execution in the specified minute across
     * the segments currently assigned to this scheduler instance.
     *
     * @param executionMinute the execution time (in epoch minutes) for which jobs
     *                        should be fetched
     * @return a list of scheduled tasks due for execution
     */
    private List<TaskSchedule> getJobsForExecution(long executionMinute) {
        logger.info("Current minute: {}", executionMinute);

        List<TaskSchedule> jobs = new ArrayList<>();
        for (Integer segment : assignedSegments) {

            List<TaskSchedule> tasks =
                    taskScheduleRepository.findJobsForCurrentMinute(
                            executionMinute,
                            segment);

            logger.debug("Fetched {} jobs for segment {}",
                    tasks.size(),
                    segment);

            jobs.addAll(tasks);
        }

        return jobs;
    }

    /**
     * Triggers the scheduler once every minute to fetch and publish jobs that
     * are due for execution.
     */
    @Scheduled(cron = "0 * * * * *") // Runs at the start of every minute
    public void fetchAndPublishJobs() {
        processMinute(Instant.now().getEpochSecond() / 60);
    }

    /**
     * Processes all scheduled jobs for the specified execution minute.
     *
     * <p>Retrieves the segments currently assigned to this scheduler instance,
     * fetches all eligible jobs for those segments, and publishes each job for
     * execution. If no segments are assigned or no jobs are due, the method
     * returns without performing any work.
     *
     * @param executionMinute the execution time (in epoch minutes) to process
     */
    private void processMinute(long executionMinute) {
        try {
            logger.info("Scheduled job triggered at: {}", Instant.now());

            assignedSegments = leaderElectionService.getAssignedSegmentsForCurrentInstance();
            if (assignedSegments == null || assignedSegments.isEmpty()) {
                logger.warn("No assigned segments. Skipping job execution.");
                return;
            }

            List<TaskSchedule> jobsToExecute = getJobsForExecution(executionMinute);
            logger.info("Jobs fetched for execution: {}", jobsToExecute.size());

            if (jobsToExecute.isEmpty()) {
                logger.info("No jobs to execute for the current minute.");
                return;
            }

            jobsToExecute.forEach(this::processTask);

        } catch (Exception e) {
            logger.error("Error fetching or publishing jobs: {}", e.getMessage(), e);
        }
    }

    private void processTask(TaskSchedule taskSchedule) {

        UUID executionId = ExecutionIdGenerator.generate(taskSchedule.getKey().getJobId(), taskSchedule.getKey().getNextExecutionTime());

        JobExecutionEvent event = JobExecutionEvent.builder()
                .executionId(executionId)
                .jobId(taskSchedule.getKey().getJobId())
                .userId(taskSchedule.getUserId())
                .jobType(taskSchedule.getJobType())
                .jobPayload(taskSchedule.getJobPayload())
                .scheduledExecutionTime(
                        Instant.ofEpochSecond(
                                taskSchedule.getKey().getNextExecutionTime() * 60L))
                .build();

        kafkaTemplate.send(kafkaTopic, event.getJobId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish JobExecutionEvent. executionId={}, jobId={}", executionId, event.getJobId(), ex);
                        return;
                    }
                    logger.info("Published job {} to partition {} offset {}", event.getJobId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());

                    rescheduleTask(taskSchedule);
                });
    }

    private void rescheduleTask(TaskSchedule taskSchedule) {

        UUID jobId = taskSchedule.getKey().getJobId();

        // One-time job
        if (!taskSchedule.isRecurring()) {

            try {
                taskScheduleRepository.delete(taskSchedule);
                scheduleLookupRepository.deleteById(jobId);
                logger.info("Removed one-time job {}", jobId);
            } catch (Exception ex) {
                rollbackOneTimeDeletion(taskSchedule);
                throw ex;
            }
            return;
        }

        Integer remainingExecutions = taskSchedule.getRemainingExecutions();

        if (remainingExecutions != null) {
            remainingExecutions--;

            if (remainingExecutions == 0) {

                taskScheduleRepository.delete(taskSchedule);
                scheduleLookupRepository.deleteById(jobId);

                logger.info(
                        "Completed recurring job {} after final execution",
                        jobId);

                return;
            }
        }

        long nextExecutionTime =
                taskSchedule.getKey().getNextExecutionTime()
                        + Duration.parse(taskSchedule.getInterval()).toMinutes();

        Instant nextExecutionInstant =
                Instant.ofEpochSecond(nextExecutionTime * 60L);

        if (taskSchedule.getEndTime() != null
                && nextExecutionInstant.isAfter(taskSchedule.getEndTime())) {

            taskScheduleRepository.delete(taskSchedule);
            scheduleLookupRepository.deleteById(jobId);

            logger.info("Stopped recurring job {} because endTime was reached", jobId);
            return;
        }

        TaskSchedule nextSchedule =
                taskScheduleMapper.copyWithNextExecutionTime(
                        taskSchedule,
                        nextExecutionTime,
                        remainingExecutions);

        ScheduleLookup newLookup = ScheduleLookup.builder()
                .jobId(jobId)
                .nextExecutionTime(nextExecutionTime)
                .segment(nextSchedule.getKey().getSegment())
                .build();

        ScheduleLookup oldLookup = ScheduleLookup.builder()
                .jobId(jobId)
                .nextExecutionTime(taskSchedule.getKey().getNextExecutionTime())
                .segment(taskSchedule.getKey().getSegment())
                .build();

        try {

            // Step 1: create the new schedule
            taskScheduleRepository.save(nextSchedule);

            // Step 2: update lookup to point to the new schedule
            scheduleLookupRepository.save(newLookup);

            // Step 3: remove the old schedule
            taskScheduleRepository.delete(taskSchedule);

            logger.info(
                    "Rescheduled job {} for {}",
                    jobId,
                    nextExecutionTime);

        } catch (Exception ex) {

            rollbackReschedule(
                    taskSchedule,
                    nextSchedule,
                    oldLookup);

            throw ex;
        }
    }

    private void rollbackReschedule(
            TaskSchedule oldSchedule,
            TaskSchedule newSchedule,
            ScheduleLookup oldLookup) {

        // Remove the new schedule if it was created
        try {
            taskScheduleRepository.delete(newSchedule);
        } catch (Exception rollbackEx) {
            logger.error(
                    "Failed to rollback new task schedule. jobId={}",
                    oldSchedule.getKey().getJobId(),
                    rollbackEx);
        }

        // Restore lookup to point to the old schedule
        try {
            scheduleLookupRepository.save(oldLookup);
        } catch (Exception rollbackEx) {
            logger.error(
                    "Failed to restore schedule lookup. jobId={}",
                    oldSchedule.getKey().getJobId(),
                    rollbackEx);
        }

        // Restore the old schedule if it was deleted
        try {
            taskScheduleRepository.save(oldSchedule);
        } catch (Exception rollbackEx) {
            logger.error(
                    "Failed to restore old task schedule. jobId={}",
                    oldSchedule.getKey().getJobId(),
                    rollbackEx);
        }
    }

    /**
     * Performs an immediate catch-up after a segment assignment change.
     *
     * <p>Processes tasks scheduled for both the previous and current minute to
     * recover any jobs that may have been missed while segment ownership was
     * being reassigned (for example, after a scheduler instance failure).
     */
    @EventListener
    public void onAssignmentChanged(AssignmentChangedEvent event) {

        logger.info("Received AssignmentChangedEvent");

        long currentMinute = Instant.now().getEpochSecond() / 60;

        processMinute(currentMinute - 1);

        processMinute(currentMinute);
    }

    private void rollbackOneTimeDeletion(TaskSchedule taskSchedule) {

        try {
            taskScheduleRepository.save(taskSchedule);

            logger.info(
                    "Restored one-time task schedule. jobId={}",
                    taskSchedule.getKey().getJobId());

        } catch (Exception ex) {

            logger.error(
                    "Failed to rollback one-time task deletion. jobId={}",
                    taskSchedule.getKey().getJobId(),
                    ex);
        }
    }
}

