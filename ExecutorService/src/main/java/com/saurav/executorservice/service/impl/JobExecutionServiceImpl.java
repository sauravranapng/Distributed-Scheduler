package com.saurav.executorservice.service.impl;


import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.executor.factory.JobExecutorFactory;
import com.saurav.executorservice.model.event.JobExecutionEvent;
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
    private final JobExecutorFactory jobExecutorFactory;

    @Override
    public void execute(JobExecutionEvent event , int attemptNumber) {
        ExecutionContext context =
                executionTrackingService.startExecution(event ,attemptNumber);

        try {

            JobExecutor executor =
                    jobExecutorFactory.getExecutor(event.getJobType());

            executor.execute(event);

            executionTrackingService.completeExecution(context);

            log.info(
                    "Job executed successfully. executionId={}, jobId={}",
                    event.getExecutionId(),
                    event.getJobId());

        } catch (Exception ex) {

            executionTrackingService.failExecution(context, ex);

            log.error(
                    "Job execution failed. executionId={}, jobId={}",
                    event.getExecutionId(),
                    event.getJobId(),
                    ex);

            throw ex;
        }
    }
}
