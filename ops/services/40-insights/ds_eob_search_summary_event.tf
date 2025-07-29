resource "aws_quicksight_data_set" "eob_search_summaries_event" {
  data_set_id = "${local.service_prefix}-eob-search-summaries-event"
  import_mode = "DIRECT_QUERY"
  name        = "AB2D EOB Search Summaries Event"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "New custom SQL"
    logical_table_map_id = "7684a8cb-e21a-468b-b1d4-594df9d08353"

    data_transforms {
      project_operation {
        projected_columns = [
          "week_start",
          "week_end",
          "contract_number",
          "job_uuid",
          "created_at",
          "completed_at",
          "since",
          "benes_searched",
          "time_to_complete",
        ]
      }
    }

    source {
      physical_table_id = "7684a8cb-e21a-468b-b1d4-594df9d08353"
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
    physical_table_map_id = "7684a8cb-e21a-468b-b1d4-594df9d08353"

    custom_sql {
      data_source_arn = aws_quicksight_data_source.aurora.arn
      name            = "New custom SQL"
      sql_query       = <<-EOT
                SELECT jv.week_start, jv.week_end, jv.contract_number, jv.job_uuid, jv.created_at, jv.completed_at, jv.since, bs.benes_searched,
                to_char(jv.completed_at - jv.created_at,'HH24:MI:SS') time_to_complete
                FROM ab2d.job_view as jv
                LEFT JOIN event.event_bene_search as bs on bs.job_id = jv.job_uuid
                ORDER BY week_start desc
            EOT

      columns {
        name = "week_start"
        type = "DATETIME"
      }
      columns {
        name = "week_end"
        type = "DATETIME"
      }
      columns {
        name = "contract_number"
        type = "STRING"
      }
      columns {
        name = "job_uuid"
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
        name = "since"
        type = "DATETIME"
      }
      columns {
        name = "benes_searched"
        type = "INTEGER"
      }
      columns {
        name = "time_to_complete"
        type = "STRING"
      }
    }
  }
}
