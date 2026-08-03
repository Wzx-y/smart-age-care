alter table care_plan_task_template
    add column version bigint not null default 0 after active;
