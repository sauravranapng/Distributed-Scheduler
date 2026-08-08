CREATE TABLE IF NOT EXISTS job_table (
                                         user_id uuid,
                                         job_id uuid,
                                         recurring boolean,
                                         job_type text,
                                         job_payload text,
                                         interval text,
                                         max_executions int,
                                         start_time timestamp,
                                         end_time timestamp,
                                         created_time timestamp,
                                         updated_time timestamp,
                                         PRIMARY KEY (user_id, job_id)
);

CREATE TABLE IF NOT EXISTS task_schedule (
                                             next_execution_time bigint,
                                             segment int,
                                             job_id uuid,
                                             user_id uuid,
                                             job_type text,
                                             job_payload text,
                                             recurring boolean,
                                             interval text,
                                             remaining_executions int,
                                             end_time timestamp,
                                             PRIMARY KEY ((next_execution_time, segment), job_id)
);

CREATE TABLE IF NOT EXISTS schedule_lookup (
                                               job_id uuid PRIMARY KEY,
                                               next_execution_time bigint,
                                               segment int
);