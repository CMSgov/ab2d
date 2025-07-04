resource "aws_quicksight_data_set" "eob_search_summaries_1" {
  data_set_id = "${local.service_prefix}-eob_search_summaries-1"
  import_mode = "DIRECT_QUERY"
  name        = "AB2D EOB Search Summaries 1"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "Intermediate Table"
    logical_table_map_id = "dcd4fa92-1bf0-3784-9689-5bf32f153edf"

    data_transforms {
      filter_operation {
        condition_expression = "NOT (locate({contract_number[job_view]},\"Z\")>0)"
      }
    }
    data_transforms {
      project_operation {
        projected_columns = [
          "id[event_bene_search]",
          "time_of_event",
          "job_id",
          "contract_number[event_bene_search]",
          "benes_expected",
          "benes_searched",
          "num_opted_out",
          "benes_errored",
          "aws_id",
          "environment",
          "organization[event_bene_search]",
          "benes_queued",
          "eobs_fetched",
          "eobs_written",
          "eob_files",
          "benes_with_eobs",
          "id[job_view]",
          "job_uuid",
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
          "organization[job_view]",
          "contract_number[job_view]",
          "contract_name",
        ]
      }
    }

    source {
      join_instruction {
        left_operand  = "d7606677-9741-445b-8472-ca669af998eb"
        on_clause     = "{job_id} = {job_uuid}"
        right_operand = "b595750f-546d-452a-8932-be68af48697f"
        type          = "RIGHT"
      }
    }
  }
  logical_table_map {
    alias                = "event_bene_search"
    logical_table_map_id = "d7606677-9741-445b-8472-ca669af998eb"

    data_transforms {
      rename_column_operation {
        column_name     = "organization"
        new_column_name = "organization[event_bene_search]"
      }
    }
    data_transforms {
      rename_column_operation {
        column_name     = "contract_number"
        new_column_name = "contract_number[event_bene_search]"
      }
    }
    data_transforms {
      rename_column_operation {
        column_name     = "id"
        new_column_name = "id[event_bene_search]"
      }
    }

    source {
      physical_table_id = "0ac4aa46-df61-3c20-86b2-c2f18098c298"
    }
  }
  logical_table_map {
    alias                = "job_view"
    logical_table_map_id = "b595750f-546d-452a-8932-be68af48697f"

    data_transforms {
      rename_column_operation {
        column_name     = "contract_number"
        new_column_name = "contract_number[job_view]"
      }
    }
    data_transforms {
      rename_column_operation {
        column_name     = "organization"
        new_column_name = "organization[job_view]"
      }
    }
    data_transforms {
      rename_column_operation {
        column_name     = "id"
        new_column_name = "id[job_view]"
      }
    }

    source {
      physical_table_id = "ab9b0ece-b9bd-3e04-9453-8dcb91c7feff"
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
    physical_table_map_id = "0ac4aa46-df61-3c20-86b2-c2f18098c298"

    relational_table {
      data_source_arn = aws_quicksight_data_source.rds.arn
      name            = "event_bene_search"
      schema          = "public"

      input_columns {
        name = "num_opted_out"
        type = "INTEGER"
      }
      input_columns {
        name = "eob_files"
        type = "INTEGER"
      }
      input_columns {
        name = "benes_errored"
        type = "INTEGER"
      }
      input_columns {
        name = "eobs_fetched"
        type = "INTEGER"
      }
      input_columns {
        name = "eobs_written"
        type = "INTEGER"
      }
      input_columns {
        name = "benes_queued"
        type = "INTEGER"
      }
      input_columns {
        name = "benes_searched"
        type = "INTEGER"
      }
      input_columns {
        name = "benes_with_eobs"
        type = "INTEGER"
      }
      input_columns {
        name = "environment"
        type = "STRING"
      }
      input_columns {
        name = "benes_expected"
        type = "INTEGER"
      }
      input_columns {
        name = "contract_number"
        type = "STRING"
      }
      input_columns {
        name = "aws_id"
        type = "STRING"
      }
      input_columns {
        name = "job_id"
        type = "STRING"
      }
      input_columns {
        name = "organization"
        type = "STRING"
      }
      input_columns {
        name = "id"
        type = "INTEGER"
      }
      input_columns {
        name = "time_of_event"
        type = "DATETIME"
      }
    }
  }

  physical_table_map {
    physical_table_map_id = "ab9b0ece-b9bd-3e04-9453-8dcb91c7feff"

    relational_table {
      data_source_arn = aws_quicksight_data_source.rds.arn
      name            = "job_view"
      schema          = "public"

      input_columns {
        name = "job_uuid"
        type = "STRING"
      }
      input_columns {
        name = "created_at"
        type = "DATETIME"
      }
      input_columns {
        name = "fhir_version"
        type = "STRING"
      }
      input_columns {
        name = "contract_name"
        type = "STRING"
      }
      input_columns {
        name = "week_start"
        type = "DATETIME"
      }
      input_columns {
        name = "request_url"
        type = "STRING"
      }
      input_columns {
        name = "year_week"
        type = "STRING"
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
        name = "output_format"
        type = "STRING"
      }
      input_columns {
        name = "contract_number"
        type = "STRING"
      }
      input_columns {
        name = "organization"
        type = "STRING"
      }
      input_columns {
        name = "id"
        type = "INTEGER"
      }
      input_columns {
        name = "week_end"
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
        name = "since"
        type = "DATETIME"
      }
    }
  }
}
