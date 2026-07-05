#Distributed Scheduler

1. Running the complete distributed system locally

Initially, I was concerned that my laptop (8 GB RAM) would not be able to run all the required components simultaneously:

JobService
SchedulingService
ExecutorService
Kafka
ZooKeeper

I reduced the JVM heap size for every Spring Boot service as well as Kafka and ZooKeeper. This allowed the complete distributed system to run comfortably on my local machine.

Lesson: Infrastructure components do not always need their default heap sizes for local development. Proper JVM tuning makes local distributed-system development feasible.

2. Kafka Client vs Kafka Broker

Initially, I assumed the Kafka Maven dependencies in .m2 meant Kafka was already installed.

I learned that:

Spring Kafka dependencies only provide Kafka client libraries.
A separate Kafka broker must still be installed and started.

This clarified the separation between an application acting as a Kafka client and the Kafka server itself.

3. Kafka KRaft Architecture

While configuring Kafka, I learned why KRaft mode introduces both a Broker and a Controller.

The Broker is responsible for:

storing topics
serving producers
serving consumers

The Controller is responsible for:

cluster metadata
leader election
partition assignments

In KRaft mode, Kafka no longer depends on ZooKeeper for its own metadata management.

4. Kafka Storage Formatting

Before starting Kafka for the first time, I had to initialize the metadata directory using:

kafka-storage format

I learned that Kafka cannot start until its metadata storage has been initialized with a Cluster ID.

This is conceptually similar to initializing a database before first use.

5. Producer/Consumer Serialization

While testing Kafka, I realized a producer sending a String cannot be consumed by a consumer expecting a JobExecutionEvent.

Both producer and consumer must agree on:

serializer
deserializer
message format

I configured JSON serialization so both services exchanged strongly typed events.

6. Kafka Topic Management

I deleted a Kafka topic while my producer and consumer services were still running.

This eventually caused broker instability and log directory failures.

Lesson:

Always stop producers and consumers before deleting or recreating Kafka topics.

ZooKeeper & Leader Election
7. ZooKeeper Path Initialization

Leader election worked immediately, but segment assignment failed with:

NoNode for /scheduling-service/instances

The issue was that my application assumed certain znodes already existed.

I learned that distributed applications should explicitly initialize required ZooKeeper paths during startup instead of assuming they exist.

8. Instance Registration

While registering scheduler instances in ZooKeeper, serialization failed because my metadata contained an Instant.

A plain Jackson ObjectMapper does not support Java Time classes by default.

Using Spring Boot's configured ObjectMapper (or registering the Java Time module) solved the problem.

Lesson:

Never create a raw ObjectMapper inside Spring applications unless necessary.

9. Debugging Distributed State

While debugging leader election, I used the ZooKeeper CLI to inspect the actual znodes.

Instead of relying only on application logs, I verified:

/
└── scheduling-service
├── leader
├── instances
└── segments

This significantly reduced debugging time.

Lesson:

When debugging distributed coordination, always inspect the coordination service directly.

10. Stale Distributed Metadata

A stale ZooKeeper znode contained:

192.168.1.6

instead of the JSON expected by my application.

This caused deserialization failures until the znode was deleted.

Lesson:

Distributed coordination stores state externally. Old metadata can survive application restarts and produce unexpected behaviour.