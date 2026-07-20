CREATE TABLE IF NOT EXISTS processed_execution (
                                                   execution_id uuid PRIMARY KEY,
                                                   job_id uuid,
                                                   processed_at timestamp
);