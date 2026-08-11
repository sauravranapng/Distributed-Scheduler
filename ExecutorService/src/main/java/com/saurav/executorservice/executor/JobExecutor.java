package com.saurav.executorservice.executor;

import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.model.event.JobExecutionEvent;

public interface JobExecutor {

    JobType supportedType();

    void execute(JobExecutionEvent event);
}