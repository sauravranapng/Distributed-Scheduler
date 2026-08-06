#Distributed Scheduler

## 1. Running the complete distributed system locally

Initially, I was concerned that my laptop (8 GB RAM) would not be able to run all the required components simultaneously:<br>

JobService<br>
SchedulingService<br>
ExecutorService<br>
Kafka<br>
ZooKeeper<br>

I reduced the JVM heap size for every Spring Boot service as well as Kafka and ZooKeeper. This allowed the complete distributed system to run comfortably on my local machine.<br>
### For Kafka
I edited the `kafka-server-start.bat` script to reduce the default heap size from 1 GB to 256 MB:<br>
`
IF ["%KAFKA_HEAP_OPTS%"] EQU [""] (
rem detect OS architecture
wmic os get osarchitecture | find /i "32-bit" >nul 2>&1
IF NOT ERRORLEVEL 1 (
set KAFKA_HEAP_OPTS=-Xmx512M -Xms512M
) ELSE (
set KAFKA_HEAP_OPTS=-Xmx1G -Xms1G
)
)`<br>
I replaced it with<br>
`IF ["%KAFKA_HEAP_OPTS%"] EQU [""] (
set KAFKA_HEAP_OPTS=-Xms128M -Xmx256M
)`<br>

### For ZooKeeper
I edited the `\bin\zkServer.cmd` script to reduce the default heap size from 1 GB to 256 MB:<br>
`call %JAVA% ^
"-Xms64m" ^
"-Xmx128m" ^
"-Dzookeeper.log.dir=%ZOO_LOG_DIR%" ^
"-Dzookeeper.log.file=%ZOO_LOG_FILE%" ^
"-XX:+HeapDumpOnOutOfMemoryError" ^
...`
Lesson: Infrastructure components do not always need their default heap sizes for local development. Proper JVM tuning makes local distributed-system development feasible.<br>

## 2. Kafka KRaft Architecture
KRaft mode introduces both a Broker and a Controller.<br>

The Broker is responsible for:<br>

storing topics<br>
serving producers<br>
serving consumers<br>

The Controller is responsible for:<br>

cluster metadata<br>
leader election<br>
partition assignments<br>

We can assign the Controller role to a Broker, but it is not required. The Controller can run on a separate node.
In KRaft mode, Kafka no longer depends on ZooKeeper for its own metadata management.<br>

## 3. Kafka Storage Formatting

Before starting Kafka for the first time, I had to initialize the metadata directory.<br>

I learned that Kafka cannot start until its metadata storage has been initialized with a Cluster ID.<br>

This is conceptually similar to initializing a database before first use.<br>
Navigate to `kafka_2.13-4.3.1\bin\windows` and <br>
run:<br>
`kafka-storage.bat random-uuid` then take this UUID(cluster-id) and <br>
run:<br>
`kafka-storage format -t <cluster-id> -c ..\..\config\kraft\server.properties`<br>
start Broker with:<br>
`kafka-server-start.bat ..\..\config\kraft\server.properties`<br>

## 6. Kafka Topic Management

I deleted a Kafka topic while my producer and consumer services were still running.<br>

This eventually caused broker instability and log directory failures.<br>

Lesson: Always stop producers and consumers before deleting or recreating Kafka topics.<br>


## 7. You can verify and inspect zookeeper znodes using the zkCli.sh command line tool.
ran `zkCli.cmd`<br>
To get content of znodes:<br>
`get /scheduling-service/segments/assignments`<br>

## 8.Set-up ZooKeeper and Kafka on Windows

### ZooKeeper
1. Download ZooKeeper from the official Apache website.<br>
2. Extract the downloaded archive to a directory of your choice.<br>
3. Navigate to the `conf` directory and create a copy of the `zoo_sample.cfg` file, renaming it to `zoo.cfg`.<br>
4. Open the `zoo.cfg` file in a text editor and configure the data directory and client port as needed.<br>
5. Start ZooKeeper by running the `zkServer.cmd` script located in the `bin` directory.<br>

### Kafka
1. Download Kafka from the official Apache website.<br>
2. Extract the downloaded archive to a directory of your choice.<br>
3. Navigate to the `config` directory and open the `server.properties` file in a text editor. Configure the necessary settings, such as broker ID, log directories, and listeners.<br>
4. generate a unique cluster ID by running the `kafka-storage.bat random-uuid` command in the `bin\windows` directory. Copy the generated UUID.<br>
5. Format the Kafka storage by running the `kafka-storage.bat format -t <cluster-id> -c ..\..\config\kraft\server.properties` command, replacing `<cluster-id>` with the UUID you generated in the previous step. This initializes the metadata storage for Kafka.<br>
6. Start Kafka by running the `kafka-server-start.bat` script located in the `bin\windows` directory, passing the path to the `server.properties` file as an argument.<br>
`kafka-server-start.bat ..\..\config\server.properties`<br>

## 9. To run docker Container of any service using local-profile and limited JVM heap size
1. Add Dockerfile + .gitignore to the root of the project and build the image<br>
   `docker build -t distributed-scheduler/jobservice:1.0 .`<br>
2. Run the container: `docker run -p 8084:8084 -e SPRING_PROFILES_ACTIVE=local -e JAVA_OPTS="-Xms128m -Xmx256m" distributed-scheduler/jobservice:1.0`<br>

## 10. Docker Concepts
### Docker Network:
"default bridge"(when you don't specify one) network->unrestricted network access to other containers ( within same  n/w ) using container IP addresses but not names.<br>
user-defined network-> containers can communicate with each other using container IP addresses or container names.<br>
For Container "A" localhost is container "A" itself.In network every container gets:an IP address+automatic DNS<br>

### Multiple Network:
A container can be connected to multiple Docker networks.For example a frontend container may be connected to a bridge network with external access, and a --internal network to communicate with containers running backend services that do not need external network access.

### IpAddress & ports:
By default, the container gets an IP address for every Docker network it attaches to.<br>
All ports of containers on bridge networks are accessible from the Docker host and other containers connected to the same network. hence need to use
-p flag to make a port available outside the host, and to containers in other bridge networks.<br>
       
## 11. Docker Compose

### Custom Kafka Image 
**Custom Kafka Docker Image**<br>

Instead of using a pre-configured Kafka distribution, this project builds a custom Kafka Docker image on top of the official apache/kafka image.<br>

The custom image consists of:<br>

`Dockerfile` – Builds the Kafka runtime image and packages the custom configuration.<br>
`server.properties` – Defines the broker configuration for running Kafka in KRaft mode.<br>
`start-kafka.sh` – Initializes Kafka storage by formatting it only on the first startup and then starts the broker.<br>
Docker Volume – Persists Kafka metadata (meta.properties) and topic logs across container restarts.<br>

**Why build a custom Kafka image?**
Building a custom image provides several advantages over using the default image as-is:<br>

1.Complete control over Kafka configuration through a version-controlled server.properties.<br>
2.Infrastructure as Code (IaC) by keeping the Dockerfile, startup scripts, and broker configuration alongside the application source code.<br>
3.Automated KRaft initialization, eliminating manual execution of kafka-storage.sh format.<br>
4.Persistent broker state using Docker volumes, ensuring metadata and topic data survive container recreation.<br>
5.Reproducible deployments, allowing any developer to build and start the same Kafka broker with a single docker compose up --build.<br>

### how to use Docker Compose
**docker compose build** :Builds or rebuilds the Docker images only. It does not create or start containers.<br>
**docker compose up** :Creates and starts containers. If an image doesn't exist, it will build it first (unless you use --no-build).<br>
**docker compose down** :Stops and removes containers, networks, but not named volumes created by docker compose up. It does not remove images unless you use the --rmi flag.<br>
**docker compose up --build** : Rebuilds the images and starts the containers. It is useful when you have made changes to the Dockerfile or application code and want to rebuild the images before starting the containers.<br>
### To check logs of a specific service
`docker compose logs <service-name>`

### To open a shell inside a running container
`docker compose exec -it <service-name> sh`
docker exec   -it   zookeeper   sh <br>
Execute a command in a running container-----Interactive terminal------Target container-----Command to run inside the container

### To run multiple instances of schedulingService using docker-compose
1.Remove the `container_name` and `hostname` directives from the schedulingService service in the docker-compose.yml file. 
This allows Docker Compose to automatically assign unique names to each instance of the service, enabling you to run multiple instances simultaneously.<br>
2.Remove port mapping for schedulingService in the docker-compose.yml file.
This prevents port conflicts when running multiple instances of the service, as each instance will use its own internal port within the Docker network.<br>
3.Run the following command to start multiple instances of schedulingService:<br>
`docker compose up --scale schedulingService=3`

Segment Assignments between multiple instances of schedulingService.
![Segment Assignments](resources/segment-assignment.png)

12 ## Future Improvements:
### 1.Integrate schema-migration tool or write one yourself.



## 13. Fault tolerance design improvements:

### Problem1: Once reassignment of segments occurs because of any instance crash before processing its complete records for that instant, the new instance will not immediately fetch record from the Db it will fetch at t+1.<br>
Normal cron: poll for currentMinute.
**Solution:**
After assignment change:immediately poll for currentMinute.

### Problem2: If re-assignment happens so close to the next minute that by the time the new instance fetches the records, the next minute has already started and the records for the previous minute are lost.<br>
Let's say the currentMinute is 10:00 and the next minute is 10:01. If the instance crash happens at 10:00:52, zookeeper identify that the instance is crashed after 5 seconds then by the time the new instance fetches records,<br>
it will be 10:01 and it will not fetch records for 10:00</br>
Can not use <= currentMinute.In Cassandra, if next_execution_time is your partition key (or the first component of it), you cannot use an inequality predicate like <= or < on it in a standard query.<br>
If you try to run a query that translates to SELECT * FROM jobs WHERE next_execution_time <= :currentMinute, Cassandra will explicitly reject it.<br>
**Solution:**
After Assignment change: poll for currentMinute and currentMinute-1.
Need to use SpringEvents otherwise interaction between leaderElectionService and schedulerService will lead to circular dependency.<br>

### Problem3: Once problem 1 solved then i need to tackle the problem of publishing duplicate record to Kafka for execution by new instance.<br>
Which will occur if old instance has published particular task_schedule record to Kafka but crashed before deleting or re-scheduling it. <br>
Then new instance will push it again because same entry is present in Db.</br>

#### Solution:1  -- **Idempotent Execution with executionId in CassandraDB -- `Exactly-Once`** 
Instead of trying to prevent duplicate publishes (which is surprisingly hard), I have made duplicate execution harmless by adding `executionId` to `JobExecutionEvent`.<br>
where executionId will be generated from (JobId + next_execution_time) so that will be same for event having these two values.<br>
Now ExecutorService can check if it has already executed a particular executionId and ignore it if it has. This way, even if the same job is published multiple times, it will only be executed once.
**Criticism :** Duplicate execution only occurs in a tiny crash window and the maximum impact is one duplicate execution per crash.
But the overhead in ExecutorService of checking in Db for executionId will be added to each and every request.

#### Solution:2  -- **Idempotent Execution with In-Memory Cache**
I'm exploiting the fact that duplicates can only originate from a very small time window. A duplicate can only come from a scheduler crash while processing the current minute.<br>
So duplicates can only be for:<br>
1.current minute <br>
2.maybe previous minute (depending on failover delay and your catch-up logic) <br>
They cannot be for jobs from yesterday or even 10 minutes ago.<br>
Suppose we keep executionId Cache with a TTL of 2 minutes.( NOt DB calls involved)<br>

**Another refinement:**
Since I already partition event by jobId , each executor only needs to remember execution IDs for the partitions it owns.<br>
Worst Case:<br>
567 executions/sec + 2-minute TTL => 567 × 120 ≈ 68,000 executionIds.<br>
70k entries( of 16 bytes each) is on the order of a few megabytes, which is very manageable. ( 1MB to 8MB)<br>
A cache of the last 2 minutes of execution IDs is likely tiny.<br>

It won't provide `exactly-once` execution, but it can eliminate almost all duplicates caused by scheduler failover without any Cassandra round-trip.<br>
Only chance of getting duplicate task_schedule executed is when scheduler fail and within 2 minutes frame Executor also fails and to the same one which is<br>
consuming the same partition which contain the duplicate record. **---Very less likely--**<br>

### Problem4:  What if the delete operation succeeds but not the insert one of update next_execution_time operation.<br>
The problem is that future executions of a recurring job disappear.
**Solution:**
Insert first then delete instead of deleting first. And Since now we have executionId in JobExecutionEvent, even if the delete operation fails and the job is published again, it will be ignored by ExecutorService as it has already executed that executionId.

### Problem5: Now because of solution of problem4 we can have 2 chain of execution of task_schedule.
task_schedule-> 10:00: pushed into Kafka & inserted task_schedule-> 10:05 but crashed before deleting task_schedule-> 10:00. <br>
Now for next Instance it will have task_schedule-> 10:00 , task_schedule-> 10:05 and both will have their chain if this was recurring task.<br>
10:00->10:05->10:10->10:15->10:20 -----------
10:05->10:10->10:15->10:20->10:25 -----------
**Solution:**
Since any task with TaskSchedulePrimaryKey(nextExecutionTime, segment, jobId) will be unique row in Cassandra so it won't <br>
let it be inserted into again into Db. So there won't be any duplicate chains.

## Schedule Lookup Table

The `task_schedule` table is optimized for the scheduler's hot path by partitioning on `(next_execution_time, segment)`, allowing efficient polling of due tasks. However, updating or deleting a scheduled job is difficult because Cassandra requires the complete primary key `(next_execution_time, segment, job_id)`, while the REST APIs only know the `job_id`.

To solve this, the scheduler maintains a lightweight `schedule_lookup` table:

| Partition Key | Columns |
|---------------|---------|
| `job_id` | `next_execution_time`, `segment` |

The lookup table is:

- Created when a job is scheduled for the first time.
- Updated whenever a recurring job is rescheduled.
- Used by update/delete APIs to locate the current `task_schedule` entry in **O(1)** time.

This design avoids expensive `ALLOW FILTERING`, secondary indexes, or full table scans while keeping the scheduler optimized for high-throughput polling.

**Why not other approaches?**

- **Quartz Scheduler** uses relational databases where triggers can be updated using SQL `UPDATE` statements. This approach is not suitable for Cassandra because primary keys are immutable.
- **Redis-based schedulers** use Sorted Sets (`ZSET`), where schedules can be updated using `ZREM` and `ZADD`. Cassandra does not provide an equivalent data structure.
- **Kafka delay queue schedulers** rely on delayed/cancellable messages instead of maintaining scheduling state. Apache Kafka does not natively support arbitrary delayed or cancellable messages.

This separation allows each table to be optimized for a single access pattern:

| Table | Responsibility |
|-------|----------------|
| `job_table` | Stores job configuration and payload |
| `task_schedule` | Optimized for scheduler polling |
| `schedule_lookup` | Fast lookup for update/delete operations |

