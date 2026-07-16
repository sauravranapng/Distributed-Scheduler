package com.saurav.jobservice.exception;

import lombok.Getter;
/*
    it is a custom exception class to handle the exception
    when a resource is not found in the table
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final String firstFieldName;
    private final String secondFieldName;
    private final Object firstFieldValue;
    private final Object secondFieldValue;
    public ResourceNotFoundException(String resourceName, String firstFieldName,String secondFieldName, Object firstFieldValue , Object secondFieldValue) {
        super(String.format("%s not found with %s : '%s' and %s :'%s'", resourceName, firstFieldName , secondFieldName , firstFieldValue, secondFieldValue));
        this.resourceName = resourceName;
        this.firstFieldName = firstFieldName;
        this.secondFieldName = secondFieldName;
        this.firstFieldValue = firstFieldValue;
        this.secondFieldValue = secondFieldValue;
    }
}