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
