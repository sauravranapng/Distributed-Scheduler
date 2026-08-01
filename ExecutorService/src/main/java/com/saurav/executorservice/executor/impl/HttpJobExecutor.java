package com.saurav.executorservice.executor.impl;

import com.saurav.executorservice.model.entity.Job;
import com.saurav.executorservice.model.enums.JobType;

import com.saurav.executorservice.executor.JobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpJobExecutor implements JobExecutor {

    @Override
    public JobType supportedType() {
        return JobType.HTTP;
    }

    @Override
    public void execute(Job job) {

        // TODO
    }
}