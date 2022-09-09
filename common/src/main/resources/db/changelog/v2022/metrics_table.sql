CREATE TABLE IF NOT EXISTS event.event_metrics
(
    id            BIGSERIAL PRIMARY KEY,
    service       varchar(32)  not null,
    time_of_event timestamp not null
)