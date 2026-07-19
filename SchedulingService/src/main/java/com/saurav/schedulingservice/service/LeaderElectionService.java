package com.saurav.schedulingservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurav.schedulingservice.model.dto.InstanceMetadata;
import com.saurav.schedulingservice.model.event.AssignmentChangedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import static com.saurav.schedulingservice.util.Constant.Z_NODE_SEPARATOR;


@Slf4j
@Component
public class LeaderElectionService {
    private final ApplicationEventPublisher eventPublisher;


    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    private String instanceId;

    @Value("${zookeeper.connect-string}")
    private String connectString;

    @Value("${zookeeper.assignment-path}")
    private String assignmentPath;

    @Value("${zookeeper.leader-path}")
    private String leaderPath;

    @Value("${zookeeper.instances-path}")
    private String instancesPath;

    @Value("${task_schedule.total-segment}")
    private Integer totalSegment;

    @Value("${server.port}")
    private int serverPort;

    private CuratorFramework client;
    private LeaderLatch leaderLatch;

    private final Map<String, List<Integer>> segmentAssignments = new ConcurrentHashMap<>();

    public LeaderElectionService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Initializes the ZooKeeper client, registers this scheduler instance,
     * participates in leader election, and starts watchers for instance and
     * segment assignment changes.
     */
    @PostConstruct
    public void start() {
        try {
            // Initialize Curator client
            client = CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(1000, 3));
            client.start();

            if (client.getState() != CuratorFrameworkState.STARTED) {
                throw new IllegalStateException("CuratorFramework failed to start.");
            }
            initializePaths();
            registerInstance();
            // Initialize leader latch
            leaderLatch = new LeaderLatch(client, leaderPath);
            leaderLatch.addListener(new LeaderLatchListener() {

                @Override
                public void isLeader() {

                    log.info("This instance is now the leader: {}", instanceId);
                    log.info("This instance is now the leader: {}", instanceId);

                    CompletableFuture.runAsync(() -> {
                        try {
                            assignSegmentsToAllInstances();
                        } catch (Exception e) {
                            log.error("Segment assignment failed", e);
                        }
                    });
                }

                @Override
                public void notLeader() {

                    log.info("Leadership lost by instance: {}", instanceId);
                }

            });
            leaderLatch.start();
            log.info("Leader election service started. Instance ID: {}", leaderLatch.getId());
            // Watch for updates
            watchInstances();
            watchSegmentAssignments();

        } catch (Exception e) {
            log.error("Error starting leader election service: {}", e.getMessage());
            stop(); // Clean up resources
        }
    }

    /**
     * Creates the required ZooKeeper znodes used by the scheduling service
     * if they do not already exist.
     *
     * @throws Exception if an error occurs while creating the znodes
     */
    private void initializePaths() throws Exception {

        createPersistentIfMissing("/scheduling-service");
        createPersistentIfMissing(instancesPath);
        createPersistentIfMissing(assignmentPath);
    }

    /**
     * Creates a persistent ZooKeeper znode if it does not already exist.
     *
     * @param path the znode path to create
     * @throws Exception if the znode cannot be created
     */
    private void createPersistentIfMissing(String path) throws Exception {

        if (client.checkExists().forPath(path) == null) {
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(path);
        }
    }

    /**
     * Determines whether the current scheduler instance is the active leader.
     *
     * @return {@code true} if this instance currently holds leadership;
     *         {@code false} otherwise
     */
    public boolean isLeader() {
        return leaderLatch.hasLeadership();
    }

    /**
     * Evenly distributes scheduler segments across all active instances and
     * persists the assignments to ZooKeeper.
     */
    private void assignSegmentsToAllInstances() {
        try {
            List<String> activeInstances = client.getChildren().forPath(instancesPath);

            if (activeInstances.isEmpty()) {
                log.warn("No active instances found for segment assignment.");
                return;
            }

            int totalSegments = totalSegment; // Number of segments
            int segmentsPerInstance = totalSegments / activeInstances.size();
            int remainder = totalSegments % activeInstances.size();

            // Distribute segments evenly among instances
            int currentSegment = 1;
            for (String instance : activeInstances) {
                List<Integer> assignedSegments = new ArrayList<>();

                for (int i = 0; i < segmentsPerInstance; i++) {
                    assignedSegments.add(currentSegment++);
                }

                if (remainder > 0) {
                    assignedSegments.add(currentSegment++);
                    remainder--;
                }

                segmentAssignments.put(instance, assignedSegments);
                log.info("Assigned segments {} to instance {}", assignedSegments, instance);
            }

            // Save segment assignments in Zookeeper
            updateSegmentAssignmentsInZookeeper();
        } catch (Exception e) {
            log.error("Error assigning segments: {}", e.getMessage());
        }
    }

    /**
     * Watches for scheduler instance registration and removal events.
     *
     * <p>When the set of active instances changes, the leader recalculates
     * and republishes segment assignments.
     */
    private void watchInstances() {

        CuratorCache instancesCache;

        instancesCache = CuratorCache.build(client, instancesPath);

        instancesCache.listenable().addListener((type, oldData, newData) -> {

            switch (type) {

                case NODE_CREATED, NODE_DELETED -> {

                    log.info("Scheduler instances changed.");

                    if (isLeader()) {
                        CompletableFuture.runAsync(this::assignSegmentsToAllInstances);
                    }
                }

                default -> {
                    // Ignore
                }
            }

        });

        instancesCache.start();
    }

    /**
     * Persists the current segment assignments to ZooKeeper so that all
     * scheduler instances can refresh their local assignment cache.
     */
    private void updateSegmentAssignmentsInZookeeper() {
        try {
            byte[] data = new ObjectMapper().writeValueAsBytes(segmentAssignments);
            // Log the serialized data to verify what is being sent to Zookeeper
            log.info("Serialized segment assignments: {}", new String(data, StandardCharsets.UTF_8));

            // Check if the path exists
            if (client.checkExists().forPath(assignmentPath) == null) {
                // Path does not exist, create it as a persistent node
                log.info("Path does not exist. Creating path at: {}", assignmentPath);

                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(assignmentPath, data);
            } else {
                // Path exists, update the data
                log.info("Path exists. Updating data at: {}", assignmentPath);

                client.setData().forPath(assignmentPath, data);
            }
        } catch (Exception e) {
            // Log the full exception to capture detailed stack trace and message
            log.error("Error updating segment assignments in Zookeeper at path {}: {}", "/scheduling-service/segments/assignments", e.getMessage(), e);
            // Optionally, you can add retry logic here in case of transient failures
        }
    }

    /**
     * Watches for updates to segment assignments stored in ZooKeeper and
     * refreshes the local assignment cache when changes occur.
     */
    private void watchSegmentAssignments() {
        CuratorCache assignmentCache;

        assignmentCache = CuratorCache.build(client, assignmentPath);

        assignmentCache.listenable().addListener((type, oldData, newData) -> {

            switch (type) {

                case NODE_CREATED, NODE_CHANGED -> {
                    try {
                        log.info("Segment assignments updated.");
                        updateLocalSegmentCache();
                    } catch (Exception e) {
                        log.error("Failed to update local segment cache", e);
                    }
                }

                default -> {
                    // Ignore other events
                }
            }
        });

        assignmentCache.start();
    }

    /**
     * Refreshes the local segment assignment cache from ZooKeeper.
     *
     * <p>If the segment assignment for the current scheduler instance changes,
     * an {@link AssignmentChangedEvent} is published to trigger immediate
     * processing of newly assigned segments.
     */
    private void updateLocalSegmentCache() {
        try {

            if (client.checkExists().forPath(assignmentPath) == null) {
                log.warn("No segment assignments found in Zookeeper.");
                segmentAssignments.clear();
                return;
            }

            byte[] data = client.getData().forPath(assignmentPath);
            Map<String, List<Integer>> updatedAssignments =
                    objectMapper.readValue(data, new TypeReference<Map<String, List<Integer>>>() {});
            List<Integer> previous =
                    segmentAssignments.getOrDefault(instanceId, Collections.emptyList());
            segmentAssignments.clear();
            segmentAssignments.putAll(updatedAssignments);
            List<Integer> current =
                    segmentAssignments.getOrDefault(instanceId, Collections.emptyList());

            if (!previous.equals(current)) {

                log.info("Assignments changed from {} to {}", previous, current);

                eventPublisher.publishEvent(new AssignmentChangedEvent());            }

            log.info("Updated local segment cache with data: {}", updatedAssignments);
        } catch (Exception e) {
            log.error("Error updating local segment cache: {}", e.getMessage());
        }
    }

    /**
     * Registers the current scheduler instance as an ephemeral node in
     * ZooKeeper so it can participate in leader election and receive
     * segment assignments.
     */
    private void registerInstance() {

        try {

            instanceId = UUID.randomUUID().toString();

            String instancePath = instancesPath + Z_NODE_SEPARATOR + instanceId;

            InstanceMetadata metadata = buildInstanceMetadata();

            byte[] data = objectMapper.writeValueAsBytes(metadata);

            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(instancePath, data);

            log.info("Registered instance {} on {}:{}",
                    metadata.getInstanceId(),
                    metadata.getHost(),
                    metadata.getPort());

        } catch (Exception e) {

            log.error("Failed to register instance", e);

        }

    }

    /**
     * Returns the segments currently assigned to this scheduler instance.
     *
     * @return the list of assigned segment identifiers, or an empty list if
     *         no segments are currently assigned
     */
    public List<Integer> getAssignedSegmentsForCurrentInstance() {
        return segmentAssignments.getOrDefault(instanceId, Collections.emptyList());
    }

    /**
     * Releases ZooKeeper resources by closing the leader latch and client
     * during application shutdown.
     */
    @PreDestroy
    public void stop() {
        try {
            if (leaderLatch != null) {
                leaderLatch.close();
            }
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            log.error("Error stopping leader election service: {}", e.getMessage());
        }
    }

    /**
     * Builds the metadata describing the current scheduler instance.
     *
     * @return the instance metadata containing the instance identifier,
     *         host address, server port, and registration timestamp
     * @throws UnknownHostException if the local host address cannot be resolved
     */
    private InstanceMetadata buildInstanceMetadata() throws UnknownHostException {

        return new InstanceMetadata(
                instanceId,
                InetAddress.getLocalHost().getHostAddress(),
                serverPort,
                Instant.now()
        );
    }
}
