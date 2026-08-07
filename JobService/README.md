## Key Design Improvements

### 1. Deterministic Segment Assignment

Replaced random segment assignment:

```java
int segment = ThreadLocalRandom.current().nextInt(1, 4);
```

with:

```java
private int calculateSegment(UUID jobId) {
    return Math.abs(jobId.hashCode()) % 100;
}
```

**Benefits:**
- Same job always maps to the same segment.
- Better workload distribution across scheduler instances.
- Supports efficient segment ownership and rebalancing.
- Scales beyond a small fixed number of segments.

---

### 2. Added `user_id` to `task_schedule`

Added `user_id` alongside `job_id` in `task_schedule`.

This allows the Scheduling Service to publish:

```java
JobExecutionEvent(userId, jobId)
```

and enables the Executor Service to fetch job details using a direct Cassandra primary-key lookup:

```sql
SELECT *
FROM job_table
WHERE user_id = ?
AND job_id = ?;
```

**Benefits:**
- No additional lookup table required.
- Efficient job retrieval during execution.
- Avoids expensive queries and scans.

---

### Impact

These changes improved:

- **Scalability** through deterministic workload distribution.
- **Execution efficiency** through direct primary-key lookups.
- **Scheduler maintainability** by making segment ownership predictable.

## Running Docker Containers 
1.Add Dockerfile + .gitignore to the root of the project and build the image:
`docker build -t distributed-scheduler/jobservice:1.0 .`
2.Run the container:
`docker run -p 8084:8084 -e SPRING_PROFILES_ACTIVE=local -e JAVA_OPTS="-Xms128m -Xmx256m" distributed-scheduler/jobservice:1.0`                         

## Cassandra does not provide multi-table transactions (hence used Saga-style compensating transactions)
CreateJob(),deleteJob(), and updateJob() must be resilient, which means that if any of the operations fail, the system must be <br>
able to recover gracefully without leaving the database in an inconsistent state. This can be achieved by implementing compensating<br>
actions or using a saga pattern to manage distributed transactions across multiple tables.<br>

I'm using rollbackJobCreation, rollbackJobUpdate, rollbackJobDeletion these methods implement a **Saga-style compensating transaction**<br>
because Cassandra does not support atomic multi-table transactions.
