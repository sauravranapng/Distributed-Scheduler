package com.saurav.executorservice.service;

import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.util.ExecutionContext;

public interface ExecutionTrackingService {


    ExecutionContext startExecution(JobExecutionEvent event);

    void completeExecution(ExecutionContext context);

    void failExecution(ExecutionContext context, Exception exception);
}