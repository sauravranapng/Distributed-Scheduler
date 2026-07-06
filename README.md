#Distributed Scheduler

## 1. Running the complete distributed system locally

Initially, I was concerned that my laptop (8 GB RAM) would not be able to run all the required components simultaneously:

JobService
SchedulingService
ExecutorService
Kafka
ZooKeeper

I reduced the JVM heap size for every Spring Boot service as well as Kafka and ZooKeeper. This allowed the complete distributed system to run comfortably on my local machine.
### For Kafka
I edited the `kafka-server-start.bat` script to reduce the default heap size from 1 GB to 256 MB:
`
IF ["%KAFKA_HEAP_OPTS%"] EQU [""] (
rem detect OS architecture
wmic os get osarchitecture | find /i "32-bit" >nul 2>&1
IF NOT ERRORLEVEL 1 (
set KAFKA_HEAP_OPTS=-Xmx512M -Xms512M
) ELSE (
set KAFKA_HEAP_OPTS=-Xmx1G -Xms1G
)
)`
I replaced it with
`IF ["%KAFKA_HEAP_OPTS%"] EQU [""] (
set KAFKA_HEAP_OPTS=-Xms128M -Xmx256M
)`

### For ZooKeeper
I edited the `\bin\zkServer.cmd` script to reduce the default heap size from 1 GB to 256 MB:
`call %JAVA% ^
"-Xms64m" ^
"-Xmx128m" ^
"-Dzookeeper.log.dir=%ZOO_LOG_DIR%" ^
"-Dzookeeper.log.file=%ZOO_LOG_FILE%" ^
"-XX:+HeapDumpOnOutOfMemoryError" ^
...`
Lesson: Infrastructure components do not always need their default heap sizes for local development. Proper JVM tuning makes local distributed-system development feasible.

## 2. Kafka KRaft Architecture
KRaft mode introduces both a Broker and a Controller.

The Broker is responsible for:

storing topics
serving producers
serving consumers

The Controller is responsible for:

cluster metadata
leader election
partition assignments

We can assign the Controller role to a Broker, but it is not required. The Controller can run on a separate node.
In KRaft mode, Kafka no longer depends on ZooKeeper for its own metadata management.

## 3. Kafka Storage Formatting

Before starting Kafka for the first time, I had to initialize the metadata directory.

I learned that Kafka cannot start until its metadata storage has been initialized with a Cluster ID.

This is conceptually similar to initializing a database before first use.
Navigate to `kafka_2.13-4.3.1\bin\windows` and run:
`kafka-storage.bat random-uuid` then take this UUID(cluster-id) and run:
`kafka-storage format -t <cluster-id> -c ..\..\config\kraft\server.properties`
start Broker with:
`kafka-server-start.bat ..\..\config\kraft\server.properties`

## 6. Kafka Topic Management

I deleted a Kafka topic while my producer and consumer services were still running.

This eventually caused broker instability and log directory failures.

Lesson: Always stop producers and consumers before deleting or recreating Kafka topics.


## 7. You can verify and inspect zookeeper znodes using the zkCli.sh command line tool.
ran `zkCli.cmd`
To get content of znodes:
`get /scheduling-service/segments/assignments`

## 8.Set-up ZooKeeper and Kafka on Windows

### ZooKeeper
1. Download ZooKeeper from the official Apache website.
2. Extract the downloaded archive to a directory of your choice.
3. Navigate to the `conf` directory and create a copy of the `zoo_sample.cfg` file, renaming it to `zoo.cfg`.
4. Open the `zoo.cfg` file in a text editor and configure the data directory and client port as needed.
5. Start ZooKeeper by running the `zkServer.cmd` script located in the `bin` directory.

### Kafka
1. Download Kafka from the official Apache website.
2. Extract the downloaded archive to a directory of your choice.
3. Navigate to the `config` directory and open the `server.properties` file in a text editor. Configure the necessary settings, such as broker ID, log directories, and listeners.
4. generate a unique cluster ID by running the `kafka-storage.bat random-uuid` command in the `bin\windows` directory. Copy the generated UUID.
5. Format the Kafka storage by running the `kafka-storage.bat format -t <cluster-id> -c ..\..\config\kraft\server.properties` command, replacing `<cluster-id>` with the UUID you generated in the previous step. This initializes the metadata storage for Kafka.
6. Start Kafka by running the `kafka-server-start.bat` script located in the `bin\windows` directory, passing the path to the `server.properties` file as an argument.   
`kafka-server-start.bat ..\..\config\server.properties`