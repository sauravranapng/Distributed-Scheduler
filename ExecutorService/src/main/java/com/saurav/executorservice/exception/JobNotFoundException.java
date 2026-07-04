package com.saurav.executorservice.exception;

import com.saurav.executorservice.entity.JobPrimaryKey;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(JobPrimaryKey primaryKey) {
        super("Job not found with primary key: " + primaryKey);
    }
}