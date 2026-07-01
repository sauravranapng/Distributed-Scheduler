CREATE TABLE IF NOT EXISTS job_table (
                                         user_id uuid,
                                         job_id uuid,
                                         recurring boolean,
                                         description text,
                                         interval text,
                                         max_retry_count int,
                                         created_time timestamp,
                                         PRIMARY KEY (user_id, job_id)
);

CREATE TABLE IF NOT EXISTS task_schedule (
                                             next_execution_time bigint,
                                             segment int,
                                             job_id uuid,
                                             user_id uuid,
                                             PRIMARY KEY ((next_execution_time, segment), job_id)
);