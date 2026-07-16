package com.saurav.jobservice.repository;


import com.saurav.jobservice.model.entity.TaskSchedule;
import com.saurav.jobservice.model.primarykey.TaskSchedulePrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskScheduleRepository extends CassandraRepository<TaskSchedule, TaskSchedulePrimaryKey> {
}
