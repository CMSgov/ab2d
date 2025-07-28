resource "aws_quicksight_data_set" "job_view" {
  data_set_id = "${local.service_prefix}-job-view"
  import_mode = "DIRECT_QUERY"
  name        = "AB2D Job View"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "job_view"
    logical_table_map_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd"

    data_transforms {
      project_operation {
        projected_columns = [
          "id",
          "job_uuid",
          "created_at",
          "completed_at",
          "expires_at",
          "resource_types",
          "status",
          "request_url",
          "output_format",
          "since",
          "until",
          "fhir_version",
          "year_week",
          "week_start",
          "week_end",
          "organization",
          "contract_number",
          "contract_name",
          "contract_type",
        ]
      }
    }

    source {
      physical_table_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd"
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
    physical_table_map_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd"

    relational_table {
      data_source_arn = aws_quicksight_data_source.aurora.arn
      name            = "job_view"
      schema          = "ab2d"

      input_columns {
        name = "id"
        type = "INTEGER"
      }
      input_columns {
        name = "job_uuid"
        type = "STRING"
      }
      input_columns {
        name = "created_at"
        type = "DATETIME"
      }
      input_columns {
        name = "completed_at"
        type = "DATETIME"
      }
      input_columns {
        name = "expires_at"
        type = "DATETIME"
      }
      input_columns {
        name = "resource_types"
        type = "STRING"
      }
      input_columns {
        name = "status"
        type = "STRING"
      }
      input_columns {
        name = "request_url"
        type = "STRING"
      }
      input_columns {
        name = "output_format"
        type = "STRING"
      }
      input_columns {
        name = "since"
        type = "DATETIME"
      }
      input_columns {
        name = "until"
        type = "DATETIME"
      }
      input_columns {
        name = "fhir_version"
        type = "STRING"
      }
      input_columns {
        name = "year_week"
        type = "STRING"
      }
      input_columns {
        name = "week_start"
        type = "DATETIME"
      }
      input_columns {
        name = "week_end"
        type = "DATETIME"
      }
      input_columns {
        name = "organization"
        type = "STRING"
      }
      input_columns {
        name = "contract_number"
        type = "STRING"
      }
      input_columns {
        name = "contract_name"
        type = "STRING"
      }
      input_columns {
        name = "contract_type"
        type = "STRING"
      }
    }
  }
}
