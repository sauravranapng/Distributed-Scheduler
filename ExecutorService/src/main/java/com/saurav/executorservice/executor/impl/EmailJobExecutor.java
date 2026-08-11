package com.saurav.executorservice.executor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.executorservice.exception.PayloadDeserializationException;
import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.model.enums.JobType;
import com.saurav.executorservice.model.event.JobExecutionEvent;
import com.saurav.executorservice.model.payload.EmailJobPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailJobExecutor implements JobExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public JobType supportedType() {
        return JobType.EMAIL;
    }

    @Override
    public void execute(JobExecutionEvent event) {

        try {
            EmailJobPayload payload = objectMapper.readValue(
                    event.getJobPayload(),
                    EmailJobPayload.class);

            log.info(
                    "Sending email. executionId={}, jobId={}, to={}, subject={}",
                    event.getExecutionId(),
                    event.getJobId(),
                    payload.getTo(),
                    payload.getSubject());

        } catch (Exception ex) {
            throw new PayloadDeserializationException(
                    "Failed to deserialize email payload",
                    ex);
        }
    }
}