package com.saurav.jobservice.config;

import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CassandraSchemaRunner {

    private final CqlSession cqlSession;

    public CassandraSchemaRunner(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @PostConstruct
    public void initSchema() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__init_schema.cql");
        String cql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        for (String statement : cql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                cqlSession.execute(trimmed);
            }
        }
    }
}