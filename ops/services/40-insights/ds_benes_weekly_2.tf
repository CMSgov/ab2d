resource "aws_quicksight_data_set" "total_benes_pulled_per_week_2_0" {
  data_set_id = "${local.service_prefix}-benes-weekly"
  import_mode = "DIRECT_QUERY"
  name        = "AB2D Total Benes Pulled Weekly"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "New custom SQL"
    logical_table_map_id = "1f75452c-8997-4f34-b7f0-51b5832866f9"

    data_transforms {
      project_operation {
        projected_columns = [
          "week_start",
          "week_end",
          "total_benes",
        ]
      }
    }

    source {
      physical_table_id = "1f75452c-8997-4f34-b7f0-51b5832866f9"
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
    physical_table_map_id = "1f75452c-8997-4f34-b7f0-51b5832866f9"

    custom_sql {
      data_source_arn = aws_quicksight_data_source.aurora.arn
      name            = "New custom SQL"
      sql_query       = <<-EOT
                select  week_start,
                        week_end,
                       sum(t.total_benes)              as total_benes
                from (SELECT jv.contract_number,
                             DATE_TRUNC('day', jv.week_start) as week_start,
                             DATE_TRUNC('day', jv.week_end)   as week_end,
                             max(bs.benes_searched)           as total_benes
                      FROM ab2d.job_view as jv
                               LEFT JOIN event.event_bene_search as bs on bs.job_id = jv.job_uuid
                      where jv.status = 'SUCCESSFUL'
                      group by jv.contract_number, jv.week_start, jv.week_end) t
                group by t.week_start, t.week_end
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
        name = "total_benes"
        type = "INTEGER"
      }
    }
  }
}
