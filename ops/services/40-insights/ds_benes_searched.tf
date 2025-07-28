resource "aws_quicksight_data_set" "benes_searched" {
  data_set_id = "${local.service_prefix}-benes-searched"
  name        = "AB2D Benes Searched In Time"
  import_mode = "DIRECT_QUERY"
  data_set_usage_configuration {
    disable_use_as_direct_query_source = false
    disable_use_as_imported_source     = false
  }

  logical_table_map {
    alias                = "${local.app}-${local.env}-benes-searched-logical"
    logical_table_map_id = "${local.app}-${local.env}-benes-searched-logical"

    data_transforms {
      create_columns_operation {
        columns {
          column_id   = "job_exec_time"
          column_name = "job_exec_time"
          expression  = "dateDiff(created_at, completed_at, \"MS\")"
        }
      }
    }
    data_transforms {
      create_columns_operation {
        columns {
          column_id   = "time_ms_per_bene"
          column_name = "time_ms_per_bene"
          expression  = "{job_exec_time} / {benes_searched}"
        }
      }
    }
    data_transforms {
      create_columns_operation {
        columns {
          column_id   = "days_of_data_searched"
          column_name = "days_of_data_searched"
          expression  = "dateDiff(data_start_time, job_start_time, \"DD\")"
        }
      }
    }
    data_transforms {
      create_columns_operation {
        columns {
          column_id   = "tm_benes_per_day"
          column_name = "tm_benes_per_day"
          expression  = "{time_ms_per_bene}/{days_of_data_searched}"
        }
      }
    }
    data_transforms {
      create_columns_operation {
        columns {
          column_id   = "tm_per_eob"
          column_name = "tm_per_eob"
          expression  = "{job_exec_time}/{eobs_written}"
        }
      }
    }
    data_transforms {
      create_columns_operation {
        columns {
          column_id   = "job_time_seconds"
          column_name = "job_time_seconds"
          expression  = "dateDiff({job_start_time}, {job_complete_time}, \"SS\")"
        }
      }
    }
    data_transforms {
      project_operation {
        projected_columns = [
          "contract_number",
          "job_uuid",
          "benes_searched",
          "created_at",
          "completed_at",
          "eobs_written",
          "data_start_time",
          "since",
          "fhir_version",
          "status",
          "Contract Number",
          "Job ID",
          "# Bene Searched",
          "Completed At",
          "# EoBs Written",
          "Data Start Date (Since Date)",
          "FHIR Version",
          "Seconds Run",
          "Job Start Time",
          "Job Complete Time",
          "sec_run",
          "job_start_time",
          "job_complete_time",
          "job_exec_time",
          "time_ms_per_bene",
          "days_of_data_searched",
          "tm_benes_per_day",
          "tm_per_eob",
          "job_time_seconds",
        ]
      }
    }

    source {
      physical_table_id = "${local.app}-${local.env}-benes-searched-physical"
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
    physical_table_map_id = "${local.app}-${local.env}-benes-searched-physical"

    custom_sql {
      data_source_arn = aws_quicksight_data_source.aurora.arn
      name            = "${local.app}-${local.env}-benes-searched"
      sql_query       = <<-EOT
                select *, contract_number as "Contract Number",
                job_uuid as "Job ID",
                benes_searched as "# Bene Searched",
                completed_at as "Completed At",
                eobs_written as "# EoBs Written",
                data_start_time as "Data Start Date (Since Date)",
                fhir_version as "FHIR Version",
                to_char(time_to_complete, 'HH24:MI:SS') as "Seconds Run",
                created_at as "Job Start Time",
                completed_at as "Job Complete Time",
                to_char(time_to_complete, 'HH24:MI:SS') as sec_run,
                created_at as job_start_time,
                completed_at as job_complete_time
                from (
                select s.contract_number, j.job_uuid, s.benes_searched, j.created_at, j.completed_at, s.eobs_written,
                j.completed_at - j.created_at as time_to_complete,
                CASE
                    WHEN j.since is null THEN
                        CASE
                            WHEN c.attested_on < '2020-01-01'
                            THEN '2020-01-01'
                            ELSE c.attested_on
                        END
                    ELSE j.since
                end as data_start_time, j.since, j.fhir_version, j.status
                from job j
                left join event.event_bene_search s on s.job_id = j.job_uuid
                left join contract_view c on c.contract_number = j.contract_number
                where j.started_by='PDP') t
                order by "Job Start Time" desc
            EOT

      columns {
        name = "contract_number"
        type = "STRING"
      }
      columns {
        name = "job_uuid"
        type = "STRING"
      }
      columns {
        name = "benes_searched"
        type = "INTEGER"
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
        name = "eobs_written"
        type = "INTEGER"
      }
      columns {
        name = "data_start_time"
        type = "DATETIME"
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
        name = "status"
        type = "STRING"
      }
      columns {
        name = "Contract Number"
        type = "STRING"
      }
      columns {
        name = "Job ID"
        type = "STRING"
      }
      columns {
        name = "# Bene Searched"
        type = "INTEGER"
      }
      columns {
        name = "Completed At"
        type = "DATETIME"
      }
      columns {
        name = "# EoBs Written"
        type = "INTEGER"
      }
      columns {
        name = "Data Start Date (Since Date)"
        type = "DATETIME"
      }
      columns {
        name = "FHIR Version"
        type = "STRING"
      }
      columns {
        name = "Seconds Run"
        type = "STRING"
      }
      columns {
        name = "Job Start Time"
        type = "DATETIME"
      }
      columns {
        name = "Job Complete Time"
        type = "DATETIME"
      }
      columns {
        name = "sec_run"
        type = "STRING"
      }
      columns {
        name = "job_start_time"
        type = "DATETIME"
      }
      columns {
        name = "job_complete_time"
        type = "DATETIME"
      }
    }
  }
}
