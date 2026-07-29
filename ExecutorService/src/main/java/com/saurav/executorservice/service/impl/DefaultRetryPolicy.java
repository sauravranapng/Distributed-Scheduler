package com.saurav.executorservice.service.impl;

import com.saurav.executorservice.exception.JobNotFoundException;
import com.saurav.executorservice.service.RetryPolicy;
import org.springframework.stereotype.Service;

@Service
public class DefaultRetryPolicy implements RetryPolicy {
     //TODO i will add later specific cases
    @Override
    public boolean shouldRetry(Exception exception) {

        return !(exception instanceof JobNotFoundException);
    }
}