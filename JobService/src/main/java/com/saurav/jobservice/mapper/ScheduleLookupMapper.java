package com.saurav.jobservice.mapper;

import com.saurav.jobservice.model.entity.ScheduleLookup;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScheduleLookupMapper {

    public ScheduleLookup toEntity(UUID jobId , long nextExecutionTime, int segment){
        ScheduleLookup lookup = new ScheduleLookup();

        lookup.setJobId(jobId);
        lookup.setNextExecutionTime(nextExecutionTime);
        lookup.setSegment(segment);
        return lookup;
    }
}
