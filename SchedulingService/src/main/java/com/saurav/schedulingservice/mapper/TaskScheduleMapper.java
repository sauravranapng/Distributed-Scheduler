package com.saurav.schedulingservice.mapper;

import com.saurav.schedulingservice.model.entity.TaskSchedule;
import com.saurav.schedulingservice.model.primarykey.TaskSchedulePrimaryKey;
import org.springframework.stereotype.Component;

@Component
public class TaskScheduleMapper {

    public TaskSchedule copyWithNextExecutionTime(
            TaskSchedule taskSchedule,
            long nextExecutionTime,
            Integer remainingExecutions) {

        TaskSchedulePrimaryKey primaryKey = new TaskSchedulePrimaryKey(
                nextExecutionTime,
                taskSchedule.getKey().getSegment(),
                taskSchedule.getKey().getJobId()
        );

        TaskSchedule nextTask = new TaskSchedule();

        nextTask.setKey(primaryKey);
        nextTask.setUserId(taskSchedule.getUserId());
        nextTask.setJobType(taskSchedule.getJobType());
        nextTask.setJobPayload(taskSchedule.getJobPayload());
        nextTask.setRecurring(taskSchedule.isRecurring());
        nextTask.setInterval(taskSchedule.getInterval());
        nextTask.setRemainingExecutions(remainingExecutions);
        nextTask.setEndTime(taskSchedule.getEndTime());

        return nextTask;
    }
}
