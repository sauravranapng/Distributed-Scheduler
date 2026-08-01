package com.saurav.executorservice.service.impl;


import com.saurav.executorservice.exception.JobNotFoundException;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.primarykey.JobPrimaryKey;
import com.saurav.executorservice.model.util.ExecutionContext;
import com.saurav.executorservice.service.ExecutionTrackingService;
import com.saurav.executorservice.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {


    private final ExecutionTrackingService executionTrackingService;

    @Override
    public void execute(JobExecutionEvent event , int attemptNumber) {
        ExecutionContext context =
                executionTrackingService.startExecution(event ,attemptNumber);

        try {
             /*
             I will add here call to execution method which will actually execute
              */
            JobPrimaryKey primaryKey = JobPrimaryKey.builder()
                    .userId(event.getUserId())
                    .jobId(event.getJobId())
                    .build();

            /*jobRepository.findById(primaryKey)
                    .orElseThrow(() -> new JobNotFoundException(primaryKey));*/

            log.info("Executing job. executionId={}, jobId={}",
                    event.getExecutionId(),
                    event.getJobId());

            executionTrackingService.completeExecution(context);

        } catch (Exception ex) {
            log.error("Job execution failed. executionId={}, jobId={}",
                    event.getExecutionId(),
                    event.getJobId(),
                    ex);

            executionTrackingService.failExecution(context, ex);

            throw ex;
        }
    }
}
