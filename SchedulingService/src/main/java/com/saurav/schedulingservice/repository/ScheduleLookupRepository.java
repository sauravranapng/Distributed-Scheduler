package com.saurav.schedulingservice.repository;

import com.saurav.schedulingservice.model.entity.ScheduleLookup;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public interface ScheduleLookupRepository extends CassandraRepository<ScheduleLookup, UUID> {
}