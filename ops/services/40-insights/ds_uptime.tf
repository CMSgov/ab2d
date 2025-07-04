resource "aws_quicksight_data_set" "uptime" {
  data_set_id = "${local.service_prefix}-uptime-all"
  name        = "AB2D All Services Uptime"
  import_mode = "DIRECT_QUERY"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "${local.app}-${local.env}-uptime-logical"
    logical_table_map_id = "${local.app}-${local.env}-uptime-logical"

    data_transforms {
      project_operation {
        projected_columns = [
          "hour",
          "uptime",
          "up",
          "event_description",
        ]
      }
    }
    source {
      physical_table_id = "${local.app}-${local.env}-uptime-physical"
    }
  }

  dynamic "permissions" {
    for_each = local.data_admins
    content {
      actions = [
        "quicksight:CancelIngestion",
        "quicksight:CreateIngestion",
        "quicksight:CreateRefreshSchedule",
        "quicksight:DeleteDataSet",
        "quicksight:DeleteDataSetRefreshProperties",
        "quicksight:DeleteRefreshSchedule",
        "quicksight:DescribeDataSet",
        "quicksight:DescribeDataSetPermissions",
        "quicksight:DescribeDataSetRefreshProperties",
        "quicksight:DescribeIngestion",
        "quicksight:DescribeRefreshSchedule",
        "quicksight:ListIngestions",
        "quicksight:ListRefreshSchedules",
        "quicksight:PassDataSet",
        "quicksight:PutDataSetRefreshProperties",
        "quicksight:UpdateDataSet",
        "quicksight:UpdateDataSetPermissions",
        "quicksight:UpdateRefreshSchedule",
      ]
      principal = "arn:aws:quicksight:us-east-1:${local.aws_account_id}:${permissions.value}"
    }
  }

  physical_table_map {
    physical_table_map_id = "${local.app}-${local.env}-uptime-physical"

    custom_sql {
      data_source_arn = aws_quicksight_data_source.rds.arn
      name            = "${local.app}-${local.env}-uptime"
      sql_query       = <<-EOT
                with ranked AS (SELECT service,
                                       state_type,
                                       event_description,
                                       time_of_event,
                                       CASE
                                           WHEN EXTRACT(epoch FROM (time_of_event - prevDate)) < 600
                                               THEN currval('metrics_sequence')
                                           ELSE nextval('metrics_sequence') END AS Rnk
                                FROM (SELECT service,
                                             state_type,
                                             event_description,
                                             time_of_event,
                                             LAG(time_of_event) OVER (ORDER BY time_of_event) AS prevDate
                                      FROM event.event_metrics) q1),
                     rankings AS (SELECT service
                                       , time_of_event
                                       , state_type
                                       , event_description
                                       , DENSE_RANK() OVER (PARTITION BY service ORDER BY time_of_event) /* ranking by the key */
                             - DENSE_RANK() OVER (PARTITION BY service, Rnk ORDER BY time_of_event) /* ranking by the key-value pair*/
                             AS sequence_grouping
                                  FROM ranked
                                  WHERE state_type = 'CONTINUE'
                                  ORDER BY time_of_event ASC),
                     data as (SELECT service
                                   , event_description
                                   , MIN(time_of_event)                      as start_date
                                   , MAX(time_of_event)                      as end_date
                                   , MAX(time_of_event) - MIN(time_of_event) as duration
                              FROM rankings
                              GROUP BY service
                                      , sequence_grouping,
                                       event_description
                              UNION ALL
                              SELECT service, event_description, start_date, end_date, end_date - start_date as duration
                              from (SELECT service,
                                           event_description,
                                           time_of_event as start_date,
                                           (select time_of_event
                                            from event.event_metrics b
                                            where b.service = a.service
                                              and state_type = 'END'
                                              and b.time_of_event >= a.time_of_event
                                            order by b.time_of_event
                                            limit 1)     as end_date
                                    FROM event.event_metrics a
                                    where state_type = 'START'
                                    group by service, state_type, event_description, time_of_event
                                    order by a.time_of_event) strstp),

                     hours as (SELECT d.hour
                               FROM generate_series(now() - interval '30 day', now(), interval '1 hour') d(hour)),
                     matches as (select m.hour
                                      , 'uptime' as uptime
                                      , count(*) AS ct
                                      , event_description
                                      , service
                                 from hours m
                                          CROSS JOIN (SELECT DISTINCT start_date FROM data) AS i -- there are faster ways
                                          CROSS JOIN LATERAL (
                                     SELECT service, event_description
                                     FROM data a
                                     WHERE a.start_date < m.hour + interval '1 hour'
                                       AND (a.end_date >= m.hour OR a.end_date IS NULL)
                                     ORDER BY a.end_date DESC -- NULLS FIRST is the default we need
                                     LIMIT 1
                                     ) a
                                 GROUP BY m.hour, a.service, event_description
                                 ORDER BY m.hour, a.service, event_description),
                     combined as (select m.hour, uptime, 0 as up
                                       , event_description
                                  from matches m
                                  union
                                  select h.hour, 'uptime' as uptime, 1 as up, '' as event_description
                                  from hours h
                                  order by hour)
                select hour, uptime, up
                     , event_description
                from (SELECT *,
                             lag(up) OVER (PARTITION BY hour ORDER BY hour, up) AS prev_year
                      FROM combined
                      group by hour, uptime, up, event_description) comp
                where comp.prev_year is null
            EOT
      columns {
        name = "hour"
        type = "DATETIME"
      }
      columns {
        name = "uptime"
        type = "STRING"
      }
      columns {
        name = "up"
        type = "INTEGER"
      }
      columns {
        name = "event_description"
        type = "STRING"
      }
    }
  }
}
