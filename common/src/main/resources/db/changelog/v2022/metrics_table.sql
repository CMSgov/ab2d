CREATE TABLE IF NOT EXISTS event.event_metrics
(
    id            BIGSERIAL PRIMARY KEY,
    service       varchar(32) not null,
    time_of_event timestamp   not null,
    awsId         varchar(255),
    environment   varchar(255),
    job_id        varchar(255)
)