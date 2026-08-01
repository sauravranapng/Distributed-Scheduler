package com.saurav.executorservice.executor.factory;

import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.model.enums.JobType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class JobExecutorFactoryImpl implements JobExecutorFactory {

    private final Map<JobType, JobExecutor> executors =
            new EnumMap<>(JobType.class);

    public JobExecutorFactoryImpl(List<JobExecutor> jobExecutors) {

        for (JobExecutor executor : jobExecutors) {
            executors.put(executor.supportedType(), executor);
        }
    }

    @Override
    public JobExecutor getExecutor(JobType jobType) {

        JobExecutor executor = executors.get(jobType);

        if (executor == null) {
            throw new IllegalArgumentException(
                    "No executor found for job type: " + jobType);
        }

        return executor;
    }
}