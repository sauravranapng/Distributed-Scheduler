package com.saurav.executorservice.service.impl;


import com.saurav.executorservice.exception.JobNotFoundException;
import com.saurav.executorservice.model.entity.Job;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.primarykey.JobPrimaryKey;
import com.saurav.executorservice.repository.JobRepository;
import com.saurav.executorservice.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {

    private final JobRepository jobRepository;

    @Override
    public void execute(JobExecutionEvent event) {

        JobPrimaryKey primaryKey = new JobPrimaryKey(
                event.getUserId(),
                event.getJobId()
        );

        Job job = jobRepository.findById(primaryKey)
                .orElseThrow(() -> new JobNotFoundException(primaryKey));

        log.info("Executing Job : {}", job.getJobPrimaryKey().getJobId());
    }
}
