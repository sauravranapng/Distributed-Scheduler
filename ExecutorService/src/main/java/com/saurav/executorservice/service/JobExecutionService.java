package com.saurav.executorservice.service;

import com.saurav.executorservice.event.JobExecutionEvent;

public interface JobExecutionService {
   void execute(JobExecutionEvent event);
}
