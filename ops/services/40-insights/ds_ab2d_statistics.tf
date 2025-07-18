resource "aws_quicksight_data_set" "ab2d_statistics" {
  data_set_id = "${local.service_prefix}-summary-statistics"
  import_mode = "DIRECT_QUERY"
  name        = "AB2D Summary Statistics"
  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "ab2d_statistics"
    logical_table_map_id = "1edadf7f-7bd5-447a-8526-785c0544f66f"

    data_transforms {
      project_operation {
        projected_columns = [
          "statistic_name",
          "statistic_value",
        ]
      }
    }

    source {
      physical_table_id = "1edadf7f-7bd5-447a-8526-785c0544f66f"
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
    physical_table_map_id = "1edadf7f-7bd5-447a-8526-785c0544f66f"

    relational_table {
      data_source_arn = aws_quicksight_data_source.aurora.arn
      name            = "ab2d_statistics"
      schema          = "ab2d"

      input_columns {
        name = "statistic_name"
        type = "STRING"
      }
      input_columns {
        name = "statistic_value"
        type = "DECIMAL"
      }
    }
  }
}
