package com.saurav.schedulingservice.service;
import com.saurav.schedulingservice.mapper.TaskScheduleMapper;
import com.saurav.schedulingservice.model.entity.TaskSchedule;
import com.saurav.schedulingservice.model.event.AssignmentChangedEvent;
import com.saurav.schedulingservice.model.event.JobExecutionEvent;
import com.saurav.schedulingservice.repository.TaskScheduleRepository;
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

@Service
public class SchedulingService {
    private final TaskScheduleMapper taskScheduleMapper;
    private final LeaderElectionService leaderElectionService;
    private final TaskScheduleRepository taskScheduleRepository;
    private final KafkaTemplate<String, JobExecutionEvent> kafkaTemplate;
    private List<Integer> assignedSegments;
    @Value("${app.kafka.topic}")
    private String kafkaTopic;
    private static final Logger logger = LoggerFactory.getLogger(SchedulingService.class);

    @Autowired
    public SchedulingService(TaskScheduleMapper taskScheduleMapper, LeaderElectionService leaderElectionService,
                             TaskScheduleRepository taskScheduleRepository, KafkaTemplate<String, JobExecutionEvent> kafkaTemplate) {
        this.taskScheduleMapper = taskScheduleMapper;
        this.leaderElectionService = leaderElectionService;
        this.taskScheduleRepository = taskScheduleRepository;
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

    /**
     * Publishes a job execution event to Kafka and, upon successful publication,
     * updates the task schedule for the next execution or removes it if it is
     * a one-time job.
     *
     * @param taskSchedule the scheduled task to publish and process
     */
    private void processTask(TaskSchedule taskSchedule) {

        JobExecutionEvent event = new JobExecutionEvent(
                taskSchedule.getUserId(),
                taskSchedule.getKey().getJobId()
        );

        kafkaTemplate.send(kafkaTopic,  event.getJobId().toString(),event)
                .whenComplete((result, ex) -> {

                    if (ex != null) {
                        logger.error("Failed to publish JobExecutionEvent: {}", event, ex);
                        return;
                    }

                    logger.info(
                            "Published event for job {} to partition {} offset {}",
                            event.getJobId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());

                    rescheduleTask(taskSchedule);

                });
    }

    /**
     * Updates the schedule of a processed task.
     *
     * <p>For one-time tasks, the schedule entry is removed. For recurring tasks,
     * the current schedule is deleted and a new schedule is created using the
     * configured execution interval.
     *
     * @param taskSchedule the processed task whose schedule should be updated
     */
    private void rescheduleTask(TaskSchedule taskSchedule) {

        if (!taskSchedule.isRecurring()) {
            taskScheduleRepository.delete(taskSchedule);

            logger.info("Removed one-time job {}",
                    taskSchedule.getKey().getJobId());
            return;
        }

        long nextExecutionTime = taskSchedule.getKey().getNextExecutionTime()
                + Duration.parse(taskSchedule.getInterval()).toMinutes();

        taskScheduleRepository.delete(taskSchedule);

        TaskSchedule nextSchedule =
                taskScheduleMapper.copyWithNextExecutionTime(
                        taskSchedule,
                        nextExecutionTime
                );

        taskScheduleRepository.save(nextSchedule);

        logger.info("Rescheduled job {} for {}",
                nextSchedule.getKey().getJobId(),
                nextExecutionTime);
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

}

