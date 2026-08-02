package com.saurav.executorservice.executor;

import com.saurav.executorservice.model.enums.JobType;

public interface JobExecutor {

    JobType supportedType();

    void execute(Job job);

}