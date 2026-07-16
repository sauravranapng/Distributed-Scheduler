package com.saurav.executorservice.service;


import com.saurav.executorservice.model.event.JobExecutionEvent;

public interface JobExecutionService {
   void execute(JobExecutionEvent event);
}
