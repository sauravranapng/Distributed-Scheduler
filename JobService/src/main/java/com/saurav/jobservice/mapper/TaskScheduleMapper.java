package com.saurav.jobservice.mapper;

import com.saurav.jobservice.model.entity.Job;
import com.saurav.jobservice.model.entity.TaskSchedule;
import com.saurav.jobservice.model.primarykey.TaskSchedulePrimaryKey;
import org.springframework.stereotype.Component;

@Component
public class TaskScheduleMapper {

    public TaskSchedule toTaskSchedule(Job job,
                                       long nextExecutionTime,
                                       int segment) {

        TaskSchedulePrimaryKey primaryKey =
                new TaskSchedulePrimaryKey(
                        nextExecutionTime,
                        segment,
                        job.getJobPrimaryKey().getJobId());

        TaskSchedule taskSchedule = new TaskSchedule();

        taskSchedule.setKey(primaryKey);
        taskSchedule.setUserId(job.getJobPrimaryKey().getUserId());
        taskSchedule.setRecurring(job.isRecurring());
        taskSchedule.setInterval(job.getInterval());

        return taskSchedule;
    }
}