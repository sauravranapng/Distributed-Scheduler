package com.saurav.jobservice.repository;

import com.saurav.jobservice.model.entity.ScheduleLookup;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public interface ScheduleLookupRepository extends CassandraRepository<ScheduleLookup, UUID> {
}
