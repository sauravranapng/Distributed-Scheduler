
CREATE TABLE IF NOT EXISTS task_execution_history (
                                                      execution_id UUID PRIMARY KEY,

                                                      job_id UUID,
                                                      user_id UUID,

                                                      scheduled_execution_time BIGINT,

                                                      execution_status TEXT,

                                                      started_at TIMESTAMP,
                                                      completed_at TIMESTAMP,
                                                      last_attempt_time TIMESTAMP,

                                                      total_attempts INT,

                                                      error_message TEXT
);

CREATE TABLE IF NOT EXISTS task_execution_attempt (

                                                      execution_id UUID,

                                                      attempt_number INT,

                                                      started_at TIMESTAMP,
                                                      completed_at TIMESTAMP,

                                                      execution_status TEXT,

                                                      retry_topic TEXT,

                                                      duration_ms BIGINT,

                                                      error_message TEXT,

                                                      lease_until timestamp,

                                                      PRIMARY KEY ((execution_id), attempt_number)

) WITH CLUSTERING ORDER BY (attempt_number ASC);