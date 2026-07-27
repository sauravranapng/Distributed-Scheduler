package com.saurav.executorservice.model.util;

import com.saurav.executorservice.model.entity.ExecutionAttempt;
import com.saurav.executorservice.model.entity.ExecutionHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {

    private ExecutionHistory executionHistory;

    private ExecutionAttempt executionAttempt;

}