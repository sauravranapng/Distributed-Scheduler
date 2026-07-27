package com.saurav.executorservice.model.primarykey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class ExecutionAttemptPrimaryKey {

    @PrimaryKeyColumn(
            name = "execution_id",
            ordinal = 0,
            type = PrimaryKeyType.PARTITIONED
    )
    private UUID executionId;

    @PrimaryKeyColumn(
            name = "attempt_number",
            ordinal = 1,
            type = PrimaryKeyType.CLUSTERED
    )
    private Integer attemptNumber;

}