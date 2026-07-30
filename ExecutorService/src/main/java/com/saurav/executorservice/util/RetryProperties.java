package com.saurav.executorservice.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.retry")
public class RetryProperties {

    private int maxAttempts;

    private Map<String, String> topics = new HashMap<>();
}