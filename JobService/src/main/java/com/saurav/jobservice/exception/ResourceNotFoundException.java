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
    private final Object firstFieldValue;

    private final String secondFieldName;
    private final Object secondFieldValue;

    // Single-field constructor
    public ResourceNotFoundException(
            String resourceName,
            String fieldName,
            Object fieldValue) {

        super(String.format(
                "%s not found with %s : '%s'",
                resourceName,
                fieldName,
                fieldValue));

        this.resourceName = resourceName;
        this.firstFieldName = fieldName;
        this.firstFieldValue = fieldValue;

        this.secondFieldName = null;
        this.secondFieldValue = null;
    }

    // Two-field constructor
    public ResourceNotFoundException(
            String resourceName,
            String firstFieldName,
            String secondFieldName,
            Object firstFieldValue,
            Object secondFieldValue) {

        super(String.format(
                "%s not found with %s : '%s' and %s : '%s'",
                resourceName,
                firstFieldName,
                firstFieldValue,
                secondFieldName,
                secondFieldValue));

        this.resourceName = resourceName;
        this.firstFieldName = firstFieldName;
        this.secondFieldName = secondFieldName;
        this.firstFieldValue = firstFieldValue;
        this.secondFieldValue = secondFieldValue;
    }
}