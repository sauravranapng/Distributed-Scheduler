package com.saurav.jobservice.repository;

import com.saurav.jobservice.entity.TaskSchedule;
import com.saurav.jobservice.util.TaskSchedulePrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskScheduleRepository extends CassandraRepository<TaskSchedule, TaskSchedulePrimaryKey> {
}
