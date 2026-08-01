package com.saurav.executorservice.executor.impl;

import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.model.entity.Job;
import com.saurav.executorservice.model.enums.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailJobExecutor implements JobExecutor {

    @Override
    public JobType supportedType() {
        return JobType.EMAIL;
    }

    @Override
    public void execute(Job job) {

        // TODO
    }
}