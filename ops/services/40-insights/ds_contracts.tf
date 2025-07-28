resource "aws_quicksight_data_set" "contracts" {
  data_set_id = "${local.service_prefix}-contracts-view"
  import_mode = "DIRECT_QUERY"
  name        = "AB2D Contracts View"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "contract_view"
    logical_table_map_id = "2496f8bb-4d1b-4cf4-8812-5bc18a2922ca"

    data_transforms {
      project_operation {
        projected_columns = [
          "contract_number",
          "contract_name",
          "attested_on",
          "created",
          "modified",
          "hpms_parent_org_name",
          "hpms_org_marketing_name",
          "update_mode",
          "contract_type",
          "enabled",
        ]
      }
    }

    source {
      physical_table_id = "2496f8bb-4d1b-4cf4-8812-5bc18a2922ca"
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
    physical_table_map_id = "2496f8bb-4d1b-4cf4-8812-5bc18a2922ca"

    relational_table {
      data_source_arn = aws_quicksight_data_source.aurora.arn
      name            = "contract_view"
      schema          = "ab2d"

      input_columns {
        name = "contract_number"
        type = "STRING"
      }
      input_columns {
        name = "contract_name"
        type = "STRING"
      }
      input_columns {
        name = "attested_on"
        type = "DATETIME"
      }
      input_columns {
        name = "created"
        type = "DATETIME"
      }
      input_columns {
        name = "modified"
        type = "DATETIME"
      }
      input_columns {
        name = "hpms_parent_org_name"
        type = "STRING"
      }
      input_columns {
        name = "hpms_org_marketing_name"
        type = "STRING"
      }
      input_columns {
        name = "update_mode"
        type = "STRING"
      }
      input_columns {
        name = "contract_type"
        type = "STRING"
      }
      input_columns {
        name = "enabled"
        type = "BIT"
      }
    }
  }
}
