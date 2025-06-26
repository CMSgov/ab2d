resource "aws_quicksight_data_set" "coverage_counts" {
  data_set_id = "6b16bc34-2db7-4614-bd91-4d9a33d65f9f"
  import_mode = "DIRECT_QUERY"
  name        = "Coverage counts"

  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "Coverage counts"
    logical_table_map_id = "bc6ff924-19e3-4405-aea1-fccfd3738ce3"

    data_transforms {
      project_operation {
        projected_columns = [
          "contract_number",
          "ab2d",
          "hpms",
          "bfd",
          "year",
          "month",
        ]
      }
    }

    source {
      physical_table_id = "a341feff-c3fe-4740-989f-e27c8f77ada0"
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
    physical_table_map_id = "a341feff-c3fe-4740-989f-e27c8f77ada0"

    custom_sql {
      data_source_arn = aws_quicksight_data_source.rds.arn
      name            = "Coverage counts"
      sql_query       = <<-EOT
                select
                	p.contract_number,
                	max (case
                		when p.service = 'AB2D' then p.count
                	end) as AB2D,
                	max(case
                		when p.service = 'HPMS' then p.count
                	end) as HPMS,
                	max(case
                		when p.service = 'BFD' then p.count
                	end) as BFD,
                	year, month

                from
                	(
                	select
                		distinct on
                		(contract_number,
                		service,
                		"year",
                		"month" ) contract_number,
                		service ,
                		create_at,
                		count,
                		"year",
                		"month"
                	from
                		lambda.coverage_counts
                	order by
                		contract_number,
                		service,
                		"year",
                		"month",
                		create_at desc
                ) p
                group by contract_number, year, month
            EOT

      columns {
        name = "contract_number"
        type = "STRING"
      }
      columns {
        name = "ab2d"
        type = "INTEGER"
      }
      columns {
        name = "hpms"
        type = "INTEGER"
      }
      columns {
        name = "bfd"
        type = "INTEGER"
      }
      columns {
        name = "year"
        type = "INTEGER"
      }
      columns {
        name = "month"
        type = "INTEGER"
      }
    }
  }
}
