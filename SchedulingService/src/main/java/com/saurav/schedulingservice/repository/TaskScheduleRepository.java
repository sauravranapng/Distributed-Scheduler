package com.saurav.schedulingservice.repository;

import com.saurav.schedulingservice.model.entity.TaskSchedule;
import com.saurav.schedulingservice.model.primarykey.TaskSchedulePrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface TaskScheduleRepository extends CassandraRepository<TaskSchedule, TaskSchedulePrimaryKey> {
    @Query("""
SELECT *
FROM task_schedule
WHERE next_execution_time = :currentMinute
AND segment = :segment
""")
    List<TaskSchedule> findJobsForCurrentMinute(
            long currentMinute,
            Integer segment);
}

