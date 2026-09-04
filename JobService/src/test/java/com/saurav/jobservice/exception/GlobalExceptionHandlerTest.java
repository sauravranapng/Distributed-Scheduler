package com.saurav.jobservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void handleResourceNotFoundException_shouldReturn404() {
        ResourceNotFoundException exception =
                new ResourceNotFoundException(
                        "Job",
                        "jobId",
                        "123"
                );

        ServletWebRequest request =
                new ServletWebRequest(
                        new MockHttpServletRequest()
                );

        var response =
                handler.handleResourceNotFoundException(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(
                exception.getMessage(),
                response.getBody().getMessage()
        );
    }

    @Test
    void handleJobServiceApiException_shouldReturn400() {
        JobServiceApiException exception =
                new JobServiceApiException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid job"
                );

        ServletWebRequest request =
                new ServletWebRequest(
                        new MockHttpServletRequest()
                );

        var response =
                handler.handleGloBazaarApiException(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(
                "Invalid job",
                response.getBody().getMessage()
        );
    }

    @Test
    void handleAnyException_shouldReturn500() {
        Exception exception =
                new RuntimeException("Unexpected error");

        ServletWebRequest request =
                new ServletWebRequest(
                        new MockHttpServletRequest()
                );

        var response =
                handler.handleAnyException(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(
                "Unexpected error",
                response.getBody().getMessage()
        );
    }

    @Test
    void handlePayloadException_shouldReturn500() {
        PayloadException exception =
                new PayloadDeserializationException(
                        "Invalid payload",
                        new RuntimeException("JSON error")
                );

        MockHttpServletRequest servletRequest =
                new MockHttpServletRequest();

        var response =
                handler.handlePayloadException(
                        exception,
                        servletRequest
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(
                "Invalid payload",
                response.getBody().getMessage()
        );
    }
}