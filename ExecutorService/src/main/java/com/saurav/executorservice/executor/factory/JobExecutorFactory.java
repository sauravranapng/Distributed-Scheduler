package com.saurav.executorservice.executor.factory;

import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.model.enums.JobType;

public interface JobExecutorFactory {

    JobExecutor getExecutor(JobType type);

}