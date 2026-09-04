package com.saurav.schedulingservice.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LeaderElectionServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LeaderElectionService service;

    @BeforeEach
    void setUp() {
        service =
                new LeaderElectionService(eventPublisher);

        ReflectionTestUtils.setField(
                service,
                "instanceId",
                "instance-1"
        );
    }

    @Test
    void getAssignedSegmentsForCurrentInstance_shouldReturnEmptyWhenNoAssignmentExists() {

        List<Integer> result =
                service.getAssignedSegmentsForCurrentInstance();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAssignedSegmentsForCurrentInstance_shouldReturnAssignedSegments() {

        ReflectionTestUtils.setField(
                service,
                "segmentAssignments",
                new java.util.concurrent.ConcurrentHashMap<>(
                        Map.of(
                                "instance-1",
                                List.of(1, 2, 3)
                        )
                )
        );

        List<Integer> result =
                service.getAssignedSegmentsForCurrentInstance();

        assertEquals(
                List.of(1, 2, 3),
                result
        );
    }

    @Test
    void getAssignedSegmentsForCurrentInstance_shouldReturnEmptyForDifferentInstance() {

        ReflectionTestUtils.setField(
                service,
                "segmentAssignments",
                new java.util.concurrent.ConcurrentHashMap<>(
                        Map.of(
                                "instance-2",
                                List.of(1, 2, 3)
                        )
                )
        );

        List<Integer> result =
                service.getAssignedSegmentsForCurrentInstance();

        assertTrue(result.isEmpty());
    }

    @Test
    void stop_shouldNotThrowWhenServiceWasNeverStarted() {

        assertDoesNotThrow(
                () -> service.stop()
        );
    }
}