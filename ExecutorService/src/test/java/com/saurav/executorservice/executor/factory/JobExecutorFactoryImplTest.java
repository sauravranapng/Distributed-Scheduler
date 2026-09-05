package com.saurav.executorservice.executor.factory;

import com.saurav.executorservice.executor.JobExecutor;
import com.saurav.executorservice.model.enums.JobType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobExecutorFactoryImplTest {

    @Test
    void getExecutor_shouldReturnExecutorForSupportedJobType() {

        JobExecutor httpExecutor = mock(JobExecutor.class);

        when(httpExecutor.supportedType())
                .thenReturn(JobType.HTTP);

        JobExecutorFactoryImpl factory =
                new JobExecutorFactoryImpl(List.of(httpExecutor));

        JobExecutor result =
                factory.getExecutor(JobType.HTTP);

        assertSame(httpExecutor, result);
    }

    @Test
    void getExecutor_shouldThrowExceptionWhenExecutorNotFound() {

        JobExecutor httpExecutor = mock(JobExecutor.class);

        when(httpExecutor.supportedType())
                .thenReturn(JobType.HTTP);

        JobExecutorFactoryImpl factory =
                new JobExecutorFactoryImpl(List.of(httpExecutor));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> factory.getExecutor(JobType.EMAIL)
                );

        assert exception.getMessage()
                .contains("No executor found for job type: EMAIL");
    }

    @Test
    void constructor_shouldRegisterMultipleExecutors() {

        JobExecutor httpExecutor = mock(JobExecutor.class);
        JobExecutor emailExecutor = mock(JobExecutor.class);

        when(httpExecutor.supportedType())
                .thenReturn(JobType.HTTP);

        when(emailExecutor.supportedType())
                .thenReturn(JobType.EMAIL);

        JobExecutorFactoryImpl factory =
                new JobExecutorFactoryImpl(
                        List.of(httpExecutor, emailExecutor));

        assertSame(
                httpExecutor,
                factory.getExecutor(JobType.HTTP));

        assertSame(
                emailExecutor,
                factory.getExecutor(JobType.EMAIL));
    }
}