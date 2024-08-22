alter table batch_executions
    add column municipality_id varchar(255);

alter table batch_executions
    add index idx_batch_executions_municipality_id (municipality_id);
