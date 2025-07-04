resource "aws_quicksight_data_set" "contracts_one_job_minimum" {
  data_set_id = "${local.service_prefix}-contracts-one-job-minimum"
  import_mode = "DIRECT_QUERY"
  name        = "AB2D Contracts, at least 1 Job"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "New custom SQL"
    logical_table_map_id = "960101d0-1c87-4a1e-8a9a-728850400b7c"

    data_transforms {
      project_operation {
        projected_columns = [
          "Contracts, at least 1 Job",
        ]
      }
    }

    source {
      physical_table_id = "960101d0-1c87-4a1e-8a9a-728850400b7c"
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
    physical_table_map_id = "960101d0-1c87-4a1e-8a9a-728850400b7c"

    custom_sql {
      data_source_arn = aws_quicksight_data_source.rds.arn
      name            = "New custom SQL"
      sql_query       = <<-EOT
                SELECT count(distinct c.contract_number) as "Contracts, at least 1 Job"
                from public.contract_view c
                inner join public.job_view j on j.contract_number = c.contract_number
                where c.contract_number not like 'Z%'
            EOT

      columns {
        name = "Contracts, at least 1 Job"
        type = "INTEGER"
      }
    }
  }
}
