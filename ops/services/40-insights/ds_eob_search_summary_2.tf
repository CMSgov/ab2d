resource "aws_quicksight_data_set" "eob_search_summaries_2" {
  data_set_id = "f93031cf-f262-46ae-af0a-c5e092264ca1"
  import_mode = "DIRECT_QUERY"
  name        = "Prod EOB Search Summaries 2"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "Prod EOB Search Summaries 2"
    logical_table_map_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e"

    data_transforms {
      project_operation {
        projected_columns = [
          "job_uuid",
          "contract_number",
          "contract_name",
          "organization",
          "created_at",
          "completed_at",
          "expires_at",
          "resource_types",
          "status",
          "request_url",
          "output_format",
          "since",
          "fhir_version",
          "year_week",
          "week_start",
          "week_end",
          "benes_expected",
          "benes_searched",
          "num_opted_out",
          "benes_errored",
          "benes_queued",
          "eobs_fetched",
          "eobs_written",
          "eob_files",
          "job_time_minutes",
          "job_time",
        ]
      }
    }

    source {
      physical_table_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e"
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
    physical_table_map_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e"

    custom_sql {
      data_source_arn = aws_quicksight_data_source.rds.arn
      name            = "Prod EOB Search Summaries 2"
      sql_query       = <<-EOT
                -- Job View related fields
                SELECT job_uuid, jv.contract_number, jv.contract_name, jv.organization, created_at, completed_at, expires_at, resource_types, status,
                    request_url, output_format, since, fhir_version,
                -- Partitioning jobs by week
                    year_week, week_start, week_end,
                -- Search summary fields
                    benes_expected, benes_searched, num_opted_out, benes_errored, benes_queued, eobs_fetched, eobs_written, eob_files,
                    extract(epoch FROM (completed_at - created_at)) / 60 AS job_time_minutes,
                    age(date_trunc('minute', completed_at), date_trunc('minute', created_at))::TEXT AS job_time
                FROM event.event_bene_search ebs
                RIGHT JOIN job_view jv ON ebs.job_id = jv.job_uuid
            EOT

      columns {
        name = "job_uuid"
        type = "STRING"
      }
      columns {
        name = "contract_number"
        type = "STRING"
      }
      columns {
        name = "contract_name"
        type = "STRING"
      }
      columns {
        name = "organization"
        type = "STRING"
      }
      columns {
        name = "created_at"
        type = "DATETIME"
      }
      columns {
        name = "completed_at"
        type = "DATETIME"
      }
      columns {
        name = "expires_at"
        type = "DATETIME"
      }
      columns {
        name = "resource_types"
        type = "STRING"
      }
      columns {
        name = "status"
        type = "STRING"
      }
      columns {
        name = "request_url"
        type = "STRING"
      }
      columns {
        name = "output_format"
        type = "STRING"
      }
      columns {
        name = "since"
        type = "DATETIME"
      }
      columns {
        name = "fhir_version"
        type = "STRING"
      }
      columns {
        name = "year_week"
        type = "STRING"
      }
      columns {
        name = "week_start"
        type = "DATETIME"
      }
      columns {
        name = "week_end"
        type = "DATETIME"
      }
      columns {
        name = "benes_expected"
        type = "INTEGER"
      }
      columns {
        name = "benes_searched"
        type = "INTEGER"
      }
      columns {
        name = "num_opted_out"
        type = "INTEGER"
      }
      columns {
        name = "benes_errored"
        type = "INTEGER"
      }
      columns {
        name = "benes_queued"
        type = "INTEGER"
      }
      columns {
        name = "eobs_fetched"
        type = "INTEGER"
      }
      columns {
        name = "eobs_written"
        type = "INTEGER"
      }
      columns {
        name = "eob_files"
        type = "INTEGER"
      }
      columns {
        name = "job_time_minutes"
        type = "DECIMAL"
      }
      columns {
        name = "job_time"
        type = "STRING"
      }
    }
  }
}
