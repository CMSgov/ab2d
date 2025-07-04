resource "aws_quicksight_analysis" "a" {
  analysis_id             = "${local.service_prefix}-dasg-metrics"
  name                    = "AB2D DASG Metrics"
  recovery_window_in_days = 30
  theme_arn               = "arn:aws:quicksight::aws:theme/CLASSIC"

  definition {
    analysis_defaults {
      default_new_sheet_configuration {
        sheet_content_type = "INTERACTIVE"

        interactive_layout_configuration {
          grid {
            canvas_size_options {
              screen_canvas_size_options {
                resize_option = "RESPONSIVE"
              }
            }
          }
        }
      }
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.uptime.name
      expression          = "1-(countIf(up,up=0)/(30*24))"
      name                = "Uptime"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.benes_searched.name
      expression          = "(dateDiff({job_start_time}, {Job Complete Time}, 'MS')/{benes_searched})"
      name                = "Average Time Per Bene (ms)"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.benes_searched.name
      expression          = "dateDiff(since, {Job Start Time}, 'DD')"
      name                = "Days of Data Searched"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.benes_searched.name
      expression          = "dateDiff({Data Start Date (Since Date)}, {Job Complete Time})"
      name                = "# Days Searched"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.benes_searched.name
      expression          = "dateDiff({Job Start Time}, {job_complete_time}, 'MS')/{eobs_written}"
      name                = "Average Time Per EoB (ms)"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.benes_searched.name
      expression          = "{# Bene Searched}"
      name                = "# Benes Searched"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.contracts.name
      expression          = "countIf({contract_number}, isNotNull({attested_on}) AND enabled = 1 AND locate({contract_number}, 'Z') <> 1)"
      name                = "Active PDPs"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.contracts.name
      expression          = "countIf({contract_number}, isNotNull({attested_on}) AND locate({contract_number}, 'Z') <> 1)/distinct_countif({contract_number}, locate({contract_number}, 'Z') <> 1)"
      name                = "Percent Attested"
    }
    calculated_fields {
      data_set_identifier = aws_quicksight_data_set.job_view.name
      expression          = "countIf({job_uuid}, status = \"SUCCESSFUL\" AND {fhir_version}=\"R4\") / countIf({job_uuid}, status = \"SUCCESSFUL\")  "
      name                = "percent r4"
    }
    column_configurations {
      role = "MEASURE"

      column {
        column_name         = "job_time"
        data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
      }
    }
    column_configurations {
      role = "MEASURE"

      column {
        column_name         = "benes_searched"
        data_set_identifier = aws_quicksight_data_set.eob_search_summaries_event.name
      }
    }

    dynamic "data_set_identifiers_declarations" {
      for_each = local.analysis_data_sets
      content {
        data_set_arn = data_set_identifiers_declarations.value.arn
        identifier   = data_set_identifiers_declarations.value.name
      }
    }

    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "cf514aaf-cc67-4ddf-a215-8de5006066dd"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "61f42b0c-f6ab-4c3b-8b75-b54bc01afb1a"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 30
          time_granularity    = "DAY"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "created_at"
            data_set_identifier = aws_quicksight_data_set.job_view.name
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "summary"
            visual_ids = [
              "5e5e4dbc-3d21-41de-85cc-730acea7cc9c",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "3a5ba838-6f5c-48f0-9e93-195056f8af07"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "7ff7926e-5901-405a-937c-bf3a9c08935a"

          column {
            column_name         = "status"
            data_set_identifier = aws_quicksight_data_set.job_view.name
          }

          configuration {
            custom_filter_configuration {
              category_value = "SUCCESSFUL"
              match_operator = "EQUALS"
              null_option    = "NON_NULLS_ONLY"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "summary"
            visual_ids = [
              "dfc1dd41-5c40-4d87-b4ff-4e90245c6ae5",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "50f40656-4451-4c1f-9aac-4d9fc2243fda"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "b7d2b7ed-36bb-4bc4-9d50-9bbe94b667cc"

          column {
            column_name         = "status"
            data_set_identifier = aws_quicksight_data_set.job_view.name
          }

          configuration {
            custom_filter_configuration {
              category_value = "SUCCESSFUL"
              match_operator = "EQUALS"
              null_option    = "NON_NULLS_ONLY"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "summary"
            visual_ids = [
              "c2100dc7-f52f-408d-b813-ede93ae79d91",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "4929e473-2b6b-441d-95f2-676d99c3fcd2"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "b02a4e70-869e-42f2-9351-f97a4d36d3d9"

          column {
            column_name         = "status"
            data_set_identifier = aws_quicksight_data_set.job_view.name
          }

          configuration {
            filter_list_configuration {
              category_values = [
                "SUCCESSFUL",
              ]
              match_operator = "CONTAINS"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "summary"
            visual_ids = [
              "26650e14-a568-4fd3-ba42-1335b54945ed",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "ce7f2635-d288-4f2b-962c-075b45ac2e99"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "b70e9291-d2d5-4a05-86da-abd6877ad829"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 3
          time_granularity    = "MONTH"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "created_at"
            data_set_identifier = aws_quicksight_data_set.job_view.name
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "summary"
            visual_ids = [
              "26650e14-a568-4fd3-ba42-1335b54945ed",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "54077dc5-ad83-4f67-ba78-61ea7a11b2b8"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "e5de9598-437f-4440-939f-2e0c7707411d"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 6
          time_granularity    = "MONTH"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "created_at"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "summary"
            visual_ids = [
              "29ffeab4-bc6c-4a0e-80c9-2122e14d2eda",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "b2f2fe80-5a88-41a8-bf24-802355e7fd18"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "2323e624-0d47-4081-a61a-02d4f500b761"
          include_maximum  = false
          include_minimum  = true
          null_option      = "NON_NULLS_ONLY"
          time_granularity = "DAY"

          column {
            column_name         = "attested_on"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          range_minimum_value {
            static_value = "2019-01-01T00:00:00Z"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "metrics"
            visual_ids = [
              "ded73292-adf4-41fc-8871-0e624951eaa6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "ca933e51-6472-4de6-90c8-a9f70d488d21"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "eb7f5276-6e0e-4087-b20f-19e5e78c7552"

          column {
            column_name         = "update_mode"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          configuration {
            filter_list_configuration {
              category_values = [
                "TEST",
              ]
              match_operator = "DOES_NOT_CONTAIN"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "metrics"
            visual_ids = [
              "ded73292-adf4-41fc-8871-0e624951eaa6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "0b0ceaaf-487d-4c10-a114-a3803692dec3"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "81ebbc21-6318-4c9f-b070-91cc1ac8806c"
          include_maximum  = false
          include_minimum  = false
          null_option      = "NULLS_ONLY"
          time_granularity = "DAY"

          column {
            column_name         = "attested_on"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "metrics"
            visual_ids = [
              "98070a89-8348-449e-bf86-7bf23e900c18",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "ALL_DATASETS"
      filter_group_id = "ee050cc3-fd77-4b5e-bd56-302721a97aec"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "d866904a-b283-4e07-ad03-cc8e95802c8a"

          column {
            column_name         = "contract_number[job_view]"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_1.name
          }

          configuration {
            filter_list_configuration {
              match_operator = "CONTAINS"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "metrics"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "ALL_DATASETS"
      filter_group_id = "8928c3d8-3f47-4758-ace6-8b8ca4aa6adf"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "fa7d1598-0b0b-48f2-ad0f-7ef8a3c4360f"

          column {
            column_name         = "fhir_version"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_1.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "metrics"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "4dcc69a7-de95-4a9d-b73a-072bdd350d3f"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "9dc36d3c-29b5-4bb8-9155-3dab4b003b21"
          include_maximum  = false
          include_minimum  = true
          null_option      = "NON_NULLS_ONLY"
          time_granularity = "DAY"

          column {
            column_name         = "since"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
          }

          range_minimum_value {
            static_value = "2020-02-13T00:00:00Z"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "metrics"
            visual_ids = [
              "51d1dbce-7a6f-4de4-8fe2-c16ae5e37635",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "ALL_DATASETS"
      filter_group_id = "fed0db8d-3136-4869-8feb-036490911824"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "b6751b4a-d774-4735-9a5a-5df1ea4b5ab9"

          column {
            column_name         = "fhir_version"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "metrics"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "ALL_DATASETS"
      filter_group_id = "f6539216-c136-4a49-bca1-33f0b14a8c6d"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "002326b5-7c31-4932-a72c-428c7a7b7b71"

          column {
            column_name         = "contract_number"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          configuration {
            filter_list_configuration {
              category_values = local.non_test_contracts
              match_operator  = "CONTAINS"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "metrics"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "da252b76-52d8-4731-96ca-a8bee0a27044"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "98ae35e7-b876-4965-904d-abb5a8bb552e"

          column {
            column_name         = "status"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "metrics"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "e6e5e94a-3309-4c68-a179-5ad9fdea6091"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "6116ab68-f161-4bdf-85e1-1de4686fafed"
          include_maximum  = false
          include_minimum  = true
          null_option      = "NON_NULLS_ONLY"
          time_granularity = "DAY"

          column {
            column_name         = "attested_on"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          range_minimum_value {
            static_value = "2019-01-01T00:00:00Z"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "attestation-records"
            visual_ids = [
              "10d42060-0022-4309-bc5f-ae0486d21f38",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "b72c4ae2-623f-4bcc-bcf7-31d55b209ea0"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "57f6288d-8489-4270-b992-62de87a11734"

          column {
            column_name         = "update_mode"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          configuration {
            filter_list_configuration {
              category_values = [
                "TEST",
              ]
              match_operator = "DOES_NOT_CONTAIN"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "attestation-records"
            visual_ids = [
              "10d42060-0022-4309-bc5f-ae0486d21f38",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "3ba238fe-2a47-4e8a-a9f4-71b4d7e5e748"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "c736da5d-323a-4d40-bbeb-9cd3fc2c725d"
          include_maximum  = false
          include_minimum  = true
          null_option      = "ALL_VALUES"
          time_granularity = "DAY"

          column {
            column_name         = "attested_on"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          range_minimum_value {
            static_value = "2019-01-01T00:00:00Z"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "attestation-records"
            visual_ids = [
              "1002e8d9-e20d-4892-8c9f-6cd51cb47908",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "552f3876-f8ba-4773-8244-7a6d61503da7"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "8eceb751-080f-4903-b370-d6c09d77364d"

          column {
            column_name         = "update_mode"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          configuration {
            filter_list_configuration {
              category_values = [
                "TEST",
              ]
              match_operator = "DOES_NOT_CONTAIN"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "attestation-records"
            visual_ids = [
              "1002e8d9-e20d-4892-8c9f-6cd51cb47908",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "ALL_DATASETS"
      filter_group_id = "3ff8296a-53f3-485a-a174-5b8b98080b4e"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "fe62d090-89fd-4031-891b-8c68349aa54a"

          column {
            column_name         = "contract_number"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          configuration {
            filter_list_configuration {
              category_values = local.non_test_contracts
              match_operator  = "CONTAINS"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "attestation-records"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "ecf8ed2b-a3d2-4c50-bfad-eedef7c48c83"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "8bcbb532-6c00-4fba-8bb3-aa1eea4f80d4"
          include_maximum  = false
          include_minimum  = true
          null_option      = "ALL_VALUES"
          time_granularity = "DAY"

          column {
            column_name         = "attested_on"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          range_minimum_value {
            static_value = "2019-01-01T00:00:00Z"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "attestation-records"
            visual_ids = [
              "a4196bd0-bb81-485d-8046-6abcce9b13a0",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "9b738079-32cb-4ad2-9ae0-4ce98738aa7e"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "cd17765a-11f8-4ced-9a74-5d7ef91f859c"

          column {
            column_name         = "update_mode"
            data_set_identifier = aws_quicksight_data_set.contracts.name
          }

          configuration {
            filter_list_configuration {
              category_values = [
                "TEST",
              ]
              match_operator = "DOES_NOT_CONTAIN"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "attestation-records"
            visual_ids = [
              "a4196bd0-bb81-485d-8046-6abcce9b13a0",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "ALL_DATASETS"
      filter_group_id = "e9956de4-c76e-479b-a05b-e21b0dc14c48"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "051c449a-2ca9-443d-973c-db77564de796"

          column {
            column_name         = "contract_number[job_view]"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_1.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "job-history"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "ALL_DATASETS"
      filter_group_id = "cd036cce-8dbf-4cad-9cd7-00391ef8ebec"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "267c424c-b5f8-4d55-9b92-b034e27509f6"

          column {
            column_name         = "contract_number"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
          }

          configuration {
            custom_filter_configuration {
              match_operator     = "DOES_NOT_CONTAIN"
              null_option        = "NON_NULLS_ONLY"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "job-history"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "35969e59-e171-4a9e-8436-8d2eb88964ed"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "712bb6be-8c28-4614-b479-ec1d7391b270"

          column {
            column_name         = "contract_number"
            data_set_identifier = aws_quicksight_data_set.eob_search_summaries_event.name
          }

          configuration {
            custom_filter_configuration {
              category_value = "Z"
              match_operator = "DOES_NOT_CONTAIN"
              null_option    = "NON_NULLS_ONLY"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "job-history"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "9a609d60-20ee-4680-af89-591ddf14780b"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "c9e5cf13-418f-4f0a-b0b6-cf1cc167610f"

          column {
            column_name         = "Contract Number"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "ALL_VISUALS"
            sheet_id = "job-history"
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "8410650d-4c9d-4482-86cc-59c5c2f2c30f"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "03d98245-021d-407f-98b6-d5862e0aab45"

          column {
            column_name         = "FHIR Version"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "avg-bene-eob-time"
            visual_ids = [
              "d26bd84e-0bc7-48d7-96f0-7172075eef9c",
              "dd13a388-dff4-44be-bba3-0de892971410",
              "fa95218c-a158-49bd-9f1b-0734c06674d6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "eafb70f0-837c-4760-b548-3b9e81ecbf47"
      status          = "ENABLED"

      filters {
        numeric_range_filter {
          filter_id       = "25cf1e9c-0e9d-4f7f-bbab-65eab5834615"
          include_maximum = false
          include_minimum = false
          null_option     = "ALL_VALUES"

          column {
            column_name         = "# Days Searched"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }

          range_minimum {
            static_value = 1
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "avg-bene-eob-time"
            visual_ids = [
              "d26bd84e-0bc7-48d7-96f0-7172075eef9c",
              "dd13a388-dff4-44be-bba3-0de892971410",
              "fa95218c-a158-49bd-9f1b-0734c06674d6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "53024702-c49d-4c47-8893-0a3673f729ec"
      status          = "ENABLED"

      filters {
        numeric_range_filter {
          filter_id       = "f1ab657e-fe79-480f-8643-190a57199312"
          include_maximum = false
          include_minimum = false
          null_option     = "NON_NULLS_ONLY"

          aggregation_function {
            numerical_aggregation_function {
              simple_numerical_aggregation = "SUM"
            }
          }

          column {
            column_name         = "Days of Data Searched"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }

          range_minimum {
            static_value = 0
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "avg-bene-eob-time"
            visual_ids = [
              "f7f17091-572a-49ff-beb6-5d037a0cfb00",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "5857ad91-9a02-4bc4-945c-c715dc4fb10d"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "9d0f751d-9064-47bf-b67c-d56b87d0847c"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 1
          time_granularity    = "YEAR"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "Job Start Time"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "avg-bene-eob-time"
            visual_ids = [
              "d26bd84e-0bc7-48d7-96f0-7172075eef9c",
              "dd13a388-dff4-44be-bba3-0de892971410",
              "f7f17091-572a-49ff-beb6-5d037a0cfb00",
              "fa95218c-a158-49bd-9f1b-0734c06674d6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "d6193b12-6821-4412-a781-e143e353206d"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "1609a46f-3966-4049-8d5d-1da5c33a2ed9"

          column {
            column_name         = "status"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "job-history"
            visual_ids = [
              "c77cd016-d4b9-47a4-8577-f2107e834d3b",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "830e2bb6-12a1-4599-a571-c24bae8253b6"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "42ccbac8-d73e-4fc1-a498-01d77cc5e775"
          include_maximum  = false
          include_minimum  = false
          null_option      = "NON_NULLS_ONLY"
          time_granularity = "DAY"

          column {
            column_name         = "Job Start Time"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "job-history"
            visual_ids = [
              "c77cd016-d4b9-47a4-8577-f2107e834d3b",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "dcca8171-4e8a-4853-8bfa-8878220c4a74"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "0b802823-e9ce-4106-a77d-021fe55ccf10"

          column {
            column_name         = "FHIR Version"
            data_set_identifier = aws_quicksight_data_set.benes_searched.name
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "job-history"
            visual_ids = [
              "c77cd016-d4b9-47a4-8577-f2107e834d3b",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "54a3213b-410d-4408-b349-2c874003517a"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "b24c7a7d-0eec-427a-aec0-3f33db21ac40"

          column {
            column_name         = "year"
            data_set_identifier = aws_quicksight_data_set.coverage_counts.name # "New custom SQL"
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "coverage-counts"
            visual_ids = [
              "d01b9194-fd61-4ba2-a8ef-957a0b49c62d",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "043e5340-ee6e-45ec-a7b7-87f7a94a58d4"
      status          = "ENABLED"

      filters {
        numeric_equality_filter {
          filter_id      = "0c11379c-efdd-4439-a342-12895256f956"
          match_operator = "EQUALS"
          null_option    = "ALL_VALUES"
          value          = 2023

          column {
            column_name         = "year"
            data_set_identifier = aws_quicksight_data_set.coverage_counts.name # "New custom SQL"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "coverage-counts"
            visual_ids = [
              "d01b9194-fd61-4ba2-a8ef-957a0b49c62d",
            ]
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Summary"
      sheet_id     = "summary"

      layouts {
        configuration {
          grid_layout {
            canvas_size_options {
              screen_canvas_size_options {
                resize_option = "RESPONSIVE"
              }
            }
            elements {
              column_index = "0"
              column_span  = 7
              element_id   = "total-beneficiaries-served"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "7"
              column_span  = 7
              element_id   = "8323c245-a824-47e3-ad70-1d9a2e509d4a"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "14"
              column_span  = 7
              element_id   = "5e5e4dbc-3d21-41de-85cc-730acea7cc9c"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "21"
              column_span  = 7
              element_id   = "12006be1-1b3f-462a-ab09-0604bc0f86a8"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "0"
              column_span  = 7
              element_id   = "538745fb-62e9-4ebf-93f2-3ca9b60a2527"
              element_type = "VISUAL"
              row_index    = "5"
              row_span     = 5
            }
            elements {
              column_index = "7"
              column_span  = 7
              element_id   = "7370839a-c922-47f9-996a-c028f195bb01"
              element_type = "VISUAL"
              row_index    = "5"
              row_span     = 5
            }
            elements {
              column_index = "14"
              column_span  = 7
              element_id   = "dfc1dd41-5c40-4d87-b4ff-4e90245c6ae5"
              element_type = "VISUAL"
              row_index    = "5"
              row_span     = 5
            }
            elements {
              column_index = "21"
              column_span  = 7
              element_id   = "c2100dc7-f52f-408d-b813-ede93ae79d91"
              element_type = "VISUAL"
              row_index    = "5"
              row_span     = 5
            }
            elements {
              column_index = "0"
              column_span  = 14
              element_id   = "29ffeab4-bc6c-4a0e-80c9-2122e14d2eda"
              element_type = "VISUAL"
              row_index    = "10"
              row_span     = 11
            }
            elements {
              column_index = "14"
              column_span  = 14
              element_id   = "26650e14-a568-4fd3-ba42-1335b54945ed"
              element_type = "VISUAL"
              row_index    = "10"
              row_span     = 11
            }
            elements {
              column_span  = 18
              element_id   = "9a5d6d1d-266b-43f4-b2e0-896d8dc643d0"
              element_type = "VISUAL"
              row_span     = 12
            }
          }
        }
      }

      visuals {
        kpi_visual {
          visual_id = "c2100dc7-f52f-408d-b813-ede93ae79d91"

          chart_configuration {
            field_wells {
              values {
                numerical_measure_field {
                  field_id = "04981fc9-c279-47b3-916f-22c1c51807b7.0.1738263051569"

                  column {
                    column_name         = "percent r4"
                    data_set_identifier = aws_quicksight_data_set.job_view.name
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">
                                    <inline color="#172b4d" font-size="14px">Percent of Calls Using R4</inline>
                                  </block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "total-beneficiaries-served"

          chart_configuration {
            field_wells {
              values {
                numerical_measure_field {
                  field_id = "1edadf7f-7bd5-447a-8526-785c0544f66f.statistic_value.1.1635886310139"

                  aggregation_function {
                    simple_numerical_aggregation = "SUM"
                  }

                  column {
                    column_name         = "statistic_value"
                    data_set_identifier = aws_quicksight_data_set.ab2d_statistics.name
                  }
                }
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">Total Beneficiaries Served</block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "8323c245-a824-47e3-ad70-1d9a2e509d4a"

          chart_configuration {
            field_wells {
              values {
                numerical_measure_field {
                  field_id = "960101d0-1c87-4a1e-8a9a-728850400b7c.Contracts, at least 1 Job.0.1669918179323"

                  aggregation_function {
                    simple_numerical_aggregation = "SUM"
                  }

                  column {
                    column_name         = "Contracts, at least 1 Job"
                    data_set_identifier = aws_quicksight_data_set.contracts_one_job_minimum.name
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">Total Entities Served</block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "7370839a-c922-47f9-996a-c028f195bb01"

          chart_configuration {
            field_wells {
              values {
                numerical_measure_field {
                  field_id = "f0cbd3f5-0068-43f6-a39a-e43ef77d3470.0.1669918234073"

                  column {
                    column_name         = "Percent Attested"
                    data_set_identifier = aws_quicksight_data_set.contracts.name
                  }

                  format_configuration {
                    numeric_format_configuration {
                      percentage_display_format_configuration {
                        null_value_format_configuration {
                          null_string = "null"
                        }
                      }
                    }
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">Total Attestation Rate</block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "538745fb-62e9-4ebf-93f2-3ca9b60a2527"

          chart_configuration {
            field_wells {
              values {
                numerical_measure_field {
                  field_id = "7bab8a95-47ff-42ea-aab7-f98fbdb47fdc.1.1685987512702"

                  column {
                    column_name         = "Active PDPs"
                    data_set_identifier = aws_quicksight_data_set.contracts.name
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">
                                    <inline color="#172b4d" font-size="14px">Total Active Attestations</inline>
                                  </block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "5e5e4dbc-3d21-41de-85cc-730acea7cc9c"

          chart_configuration {
            field_wells {
              values {
                categorical_measure_field {
                  aggregation_function = "DISTINCT_COUNT"
                  field_id             = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_number.0.1671736075647"

                  column {
                    column_name         = "contract_number"
                    data_set_identifier = aws_quicksight_data_set.job_view.name
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">Active PDPs</block>
                                  <br/>
                                  <block align="center">
                                    <inline font-size="12px">(Started a job in the last 30 days)</inline>
                                  </block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "dfc1dd41-5c40-4d87-b4ff-4e90245c6ae5"

          chart_configuration {
            field_wells {
              values {
                categorical_measure_field {
                  aggregation_function = "COUNT"
                  field_id             = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.job_uuid.0.1671742088789"

                  column {
                    column_name         = "job_uuid"
                    data_set_identifier = aws_quicksight_data_set.job_view.name
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">
                                    <inline color="#172b4d" font-size="14px">Total Number of Completed Jobs</inline>
                                  </block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "12006be1-1b3f-462a-ab09-0604bc0f86a8"

          chart_configuration {
            field_wells {
              values {
                numerical_measure_field {
                  field_id = "bff42624-f63a-48c8-b44d-09272f354c90.1.1673456264771"

                  column {
                    column_name         = "Uptime"
                    data_set_identifier = aws_quicksight_data_set.uptime.name
                  }

                  format_configuration {
                    numeric_format_configuration {
                      percentage_display_format_configuration {
                        null_value_format_configuration {
                          null_string = "null"
                        }
                      }
                    }
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <block align="center">Total API Uptime</block>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        bar_chart_visual {
          visual_id = "26650e14-a568-4fd3-ba42-1335b54945ed"

          chart_configuration {
            bars_arrangement = "STACKED"
            orientation      = "VERTICAL"

            category_axis {
              scrollbar_options {
                visible_range {
                  percent_range {
                    from = 0
                    to   = 100
                  }
                }
              }
            }

            category_label_options {
              axis_label_options {
                custom_label = "Contracts"

                apply_to {
                  field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_number.0.1675897803589"

                  column {
                    column_name         = "contract_number"
                    data_set_identifier = aws_quicksight_data_set.job_view.name
                  }
                }
              }
            }

            data_labels {
              overlap    = "DISABLE_OVERLAP"
              visibility = "HIDDEN"
            }

            field_wells {
              bar_chart_aggregated_field_wells {
                category {
                  categorical_dimension_field {
                    field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_number.0.1675897803589"

                    column {
                      column_name         = "contract_number"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
                values {
                  categorical_measure_field {
                    aggregation_function = "COUNT"
                    field_id             = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.job_uuid.1.1675897947257"

                    column {
                      column_name         = "job_uuid"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
              }
            }

            sort_configuration {
              category_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_number.0.1675897803589"
                }
              }
            }

            tooltip {
              selected_tooltip_type = "DETAILED"
              tooltip_visibility    = "VISIBLE"

              field_base_tooltip {
                aggregation_visibility = "HIDDEN"
                tooltip_title_type     = "PRIMARY_VALUE"

                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_number.0.1675897803589"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.job_uuid.1.1675897947257"
                    label      = "Job count"
                    visibility = "VISIBLE"
                  }
                }
              }
            }

            value_label_options {
              axis_label_options {
                custom_label = "Jobs completed"

                apply_to {
                  field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.job_uuid.1.1675897947257"

                  column {
                    column_name         = "job_uuid"
                    data_set_identifier = aws_quicksight_data_set.job_view.name
                  }
                }
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline color="#172b4d" font-size="14px">API Activity (PDP job count)</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        combo_chart_visual {
          visual_id = "29ffeab4-bc6c-4a0e-80c9-2122e14d2eda"

          chart_configuration {
            bars_arrangement = "CLUSTERED"

            bar_data_labels {
              overlap    = "DISABLE_OVERLAP"
              visibility = "HIDDEN"
            }

            category_axis {
              scrollbar_options {
                visible_range {
                  percent_range {
                    from = 0
                    to   = 100
                  }
                }
              }
            }

            category_label_options {
              axis_label_options {
                custom_label = "Week"

                apply_to {
                  field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.created_at.1.1676044290417"

                  column {
                    column_name         = "created_at"
                    data_set_identifier = aws_quicksight_data_set.benes_searched.name
                  }
                }
              }
            }

            field_wells {
              combo_chart_aggregated_field_wells {
                bar_values {
                  categorical_measure_field {
                    aggregation_function = "COUNT"
                    field_id             = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job ID.2.1676044306160"

                    column {
                      column_name         = "Job ID"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                category {
                  date_dimension_field {
                    field_id     = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.created_at.1.1676044290417"
                    hierarchy_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.created_at.1.1676044290417"

                    column {
                      column_name         = "created_at"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                line_values {
                  numerical_measure_field {
                    field_id = "8186ff79-fc98-4658-90d3-4da5ee6c1d1c.2.1676044125882"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "# Benes Searched"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
              }
            }

            legend {
              width = "100px"
            }

            primary_y_axis_display_options {
              grid_line_visibility = "VISIBLE"

            }

            primary_y_axis_label_options {
              axis_label_options {
                custom_label = "Job count"

                apply_to {
                  field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job ID.2.1676044306160"

                  column {
                    column_name         = "Job ID"
                    data_set_identifier = aws_quicksight_data_set.benes_searched.name
                  }
                }
              }
            }

            secondary_y_axis_display_options {
              tick_label_options {
                rotation_angle = 0

                label_options {
                  visibility = "VISIBLE"
                }
              }
            }

            secondary_y_axis_label_options {
              axis_label_options {
                custom_label = "Benes served"

                apply_to {
                  field_id = "8186ff79-fc98-4658-90d3-4da5ee6c1d1c.2.1676044125882"

                  column {
                    column_name         = "# Benes Searched"
                    data_set_identifier = aws_quicksight_data_set.benes_searched.name
                  }
                }
              }
            }

            sort_configuration {
              category_sort {
                field_sort {
                  direction = "ASC"
                  field_id  = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.created_at.1.1676044290417"
                }
              }
            }

            tooltip {
              selected_tooltip_type = "DETAILED"
              tooltip_visibility    = "VISIBLE"

              field_base_tooltip {
                aggregation_visibility = "HIDDEN"
                tooltip_title_type     = "PRIMARY_VALUE"

                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "8186ff79-fc98-4658-90d3-4da5ee6c1d1c.2.1676044125882"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.created_at.1.1676044290417"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job ID.2.1676044306160"
                    visibility = "VISIBLE"
                  }
                }
              }
            }
          }

          column_hierarchies {
            date_time_hierarchy {
              hierarchy_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.created_at.1.1676044290417"
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>Jobs and Benes served per week</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "9a5d6d1d-266b-43f4-b2e0-896d8dc643d0"

          chart_configuration {
            field_options {
              selected_field_options {
                field_id   = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_name.1.1714492190549"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_number.2.1714492190549"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.completed_at.0.1714492190549"
                visibility = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.completed_at.0.1714492190549"

                    column {
                      column_name         = "completed_at"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_name.1.1714492190549"

                    column {
                      column_name         = "contract_name"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.contract_number.2.1714492190549"

                    column {
                      column_name         = "contract_number"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
              }
            }
            sort_configuration {
            }
            table_options {
              collapsed_row_dimensions_visibility = "HIDDEN"

              row_alternate_color_options {
                status = "DISABLED"
              }

            }
            total_options {
              column_total_options {
                placement = "END"
              }
              row_subtotal_options {
                custom_label      = "<<$aws:subtotalDimension>> Subtotal"
                totals_visibility = "VISIBLE"
              }
              row_total_options {
                placement = "AUTO"
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Metrics"
      sheet_id     = "metrics"

      filter_controls {
        dropdown {
          filter_control_id = "85fd04f5-c682-4166-b476-00e502be8f79"
          source_filter_id  = "b6751b4a-d774-4735-9a5a-5df1ea4b5ab9"
          title             = "Job FHIR Version"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "99acab06-6021-46d8-bac7-38b447dcf482"
          source_filter_id  = "002326b5-7c31-4932-a72c-428c7a7b7b71"
          title             = "Contract Number"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "6fa82d62-8cd1-406b-99e8-fbdeaa3fe1c4"
          source_filter_id  = "98ae35e7-b876-4965-904d-abb5a8bb552e"
          title             = "Job Status"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 12
              element_id   = "bb9bf112-12a3-4479-94ba-3f8aaf18185d"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 14
            }
            elements {
              column_index = "12"
              column_span  = 12
              element_id   = "f22e697d-c198-4c57-b723-441bb0099b1f"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 14
            }
            elements {
              column_index = "24"
              column_span  = 12
              element_id   = "d3083896-0bfa-481d-abdf-80b2196adf9d"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "24"
              column_span  = 5
              element_id   = "99acab06-6021-46d8-bac7-38b447dcf482"
              element_type = "FILTER_CONTROL"
              row_index    = "5"
              row_span     = 5
            }
            elements {
              column_index = "29"
              column_span  = 7
              element_id   = "ded73292-adf4-41fc-8871-0e624951eaa6"
              element_type = "VISUAL"
              row_index    = "5"
              row_span     = 5
            }
            elements {
              column_index = "24"
              column_span  = 5
              element_id   = "85fd04f5-c682-4166-b476-00e502be8f79"
              element_type = "FILTER_CONTROL"
              row_index    = "10"
              row_span     = 5
            }
            elements {
              column_index = "29"
              column_span  = 7
              element_id   = "98070a89-8348-449e-bf86-7bf23e900c18"
              element_type = "VISUAL"
              row_index    = "10"
              row_span     = 5
            }
            elements {
              column_index = "0"
              column_span  = 12
              element_id   = "baf3dc66-717a-42c6-9c40-9f22321957d6"
              element_type = "VISUAL"
              row_index    = "14"
              row_span     = 12
            }
            elements {
              column_index = "12"
              column_span  = 12
              element_id   = "51d1dbce-7a6f-4de4-8fe2-c16ae5e37635"
              element_type = "VISUAL"
              row_index    = "14"
              row_span     = 12
            }
            elements {
              column_index = "24"
              column_span  = 9
              element_id   = "6fa82d62-8cd1-406b-99e8-fbdeaa3fe1c4"
              element_type = "FILTER_CONTROL"
              row_index    = "15"
              row_span     = 5
            }
            elements {
              column_index = "0"
              column_span  = 12
              element_id   = "5a2bcd55-da4d-4ffd-b697-e7279e917dc1"
              element_type = "VISUAL"
              row_index    = "26"
              row_span     = 13
            }
          }
        }
      }

      visuals {
        kpi_visual {
          visual_id = "ded73292-adf4-41fc-8871-0e624951eaa6"

          chart_configuration {
            field_wells {
              values {
                date_measure_field {
                  aggregation_function = "COUNT"
                  field_id             = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.attested_on.0.1634061471673"

                  column {
                    column_name         = "attested_on"
                    data_set_identifier = aws_quicksight_data_set.contracts.name
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "Attested Contracts"
            }
          }
        }
      }
      visuals {
        kpi_visual {
          visual_id = "98070a89-8348-449e-bf86-7bf23e900c18"

          chart_configuration {
            field_wells {
              values {
                categorical_measure_field {
                  aggregation_function = "COUNT"
                  field_id             = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_number.1.1634062873426"

                  column {
                    column_name         = "contract_number"
                    data_set_identifier = aws_quicksight_data_set.contracts.name
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "Unattested Contracts"
            }
          }
        }
      }
      visuals {
        table_visual {
          visual_id = "d3083896-0bfa-481d-abdf-80b2196adf9d"

          chart_configuration {
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  categorical_dimension_field {
                    field_id = "1edadf7f-7bd5-447a-8526-785c0544f66f.statistic_name.0.1635886310139"

                    column {
                      column_name         = "statistic_name"
                      data_set_identifier = aws_quicksight_data_set.ab2d_statistics.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "1edadf7f-7bd5-447a-8526-785c0544f66f.statistic_value.1.1635886310139"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "statistic_value"
                      data_set_identifier = aws_quicksight_data_set.ab2d_statistics.name
                    }
                  }
                }
              }
            }
            sort_configuration {
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "AB2D Summary Statistics"
            }
          }
        }
      }
      visuals {
        table_visual {
          visual_id = "baf3dc66-717a-42c6-9c40-9f22321957d6"

          chart_configuration {
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  date_dimension_field {
                    field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.1.1636039540301"

                    column {
                      column_name         = "week_start"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_end.2.1636039564714"

                    column {
                      column_name         = "week_end"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
                values {
                  categorical_measure_field {
                    aggregation_function = "DISTINCT_COUNT"
                    field_id             = "b740b36f-bfe8-45dc-b87a-82d53a44799e.contract_number.2.1636039540301"

                    column {
                      column_name         = "contract_number"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              row_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.1.1636039540301"
                }
              }
            }
            table_options {
              orientation = "VERTICAL"

              cell_style {
                height = 25
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "Unique Contracts Run Each Week"
            }
          }
        }
      }
      visuals {
        table_visual {
          visual_id = "f22e697d-c198-4c57-b723-441bb0099b1f"

          chart_configuration {
            field_options {
              order = [
                "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.1.1636039669392",
                "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_end.0.1636039669392",
                "b740b36f-bfe8-45dc-b87a-82d53a44799e.job_uuid.2.1636039669392",
                "b740b36f-bfe8-45dc-b87a-82d53a44799e.benes_searched.3.1722965528141",
              ]

              selected_field_options {
                field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.job_uuid.2.1636039669392"
                width    = "69px"
              }
            }
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  date_dimension_field {
                    field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.1.1636039669392"

                    column {
                      column_name         = "week_start"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_end.0.1636039669392"

                    column {
                      column_name         = "week_end"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
                values {
                  categorical_measure_field {
                    aggregation_function = "COUNT"
                    field_id             = "b740b36f-bfe8-45dc-b87a-82d53a44799e.job_uuid.2.1636039669392"

                    column {
                      column_name         = "job_uuid"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.benes_searched.3.1722965528141"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "benes_searched"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              row_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.1.1636039669392"
                }
              }
            }
            table_options {
              header_style {
                height    = 26
                text_wrap = "WRAP"
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "Calls Made Each Week"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "51d1dbce-7a6f-4de4-8fe2-c16ae5e37635"

          chart_configuration {
            field_options {
              selected_field_options {
                field_id   = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.0.1636039763614"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_end.1.1636039763614"
                visibility = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                rows {
                  date_dimension_field {
                    field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.0.1636039763614"

                    column {
                      column_name         = "week_start"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
                rows {
                  date_dimension_field {
                    field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_end.1.1636039763614"

                    column {
                      column_name         = "week_end"
                      data_set_identifier = aws_quicksight_data_set.eob_search_summaries_2.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.0.1636039763614"

                sort_by {
                  field {
                    direction = "DESC"
                    field_id  = "b740b36f-bfe8-45dc-b87a-82d53a44799e.week_start.0.1636039763614"
                  }
                }
              }
            }
            table_options {
              cell_style {
                height = 25
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "Calls Made Using Since"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "bb9bf112-12a3-4479-94ba-3f8aaf18185d"

          chart_configuration {
            field_options {
              data_path_options {
                width = "124px"

                data_path_list {
                  field_id    = "1f75452c-8997-4f34-b7f0-51b5832866f9.week_end.1.1654804289223"
                  field_value = "week_end"
                }
              }
              selected_field_options {
                field_id   = "1f75452c-8997-4f34-b7f0-51b5832866f9.week_start.0.1654804288199"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "1f75452c-8997-4f34-b7f0-51b5832866f9.week_end.1.1654804289223"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "1f75452c-8997-4f34-b7f0-51b5832866f9.total_benes.2.1680630577824"
                visibility = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                rows {
                  date_dimension_field {
                    field_id = "1f75452c-8997-4f34-b7f0-51b5832866f9.week_start.0.1654804288199"

                    column {
                      column_name         = "week_start"
                      data_set_identifier = aws_quicksight_data_set.total_benes_pulled_per_week_2_0.name
                    }
                  }
                }
                rows {
                  date_dimension_field {
                    field_id = "1f75452c-8997-4f34-b7f0-51b5832866f9.week_end.1.1654804289223"

                    column {
                      column_name         = "week_end"
                      data_set_identifier = aws_quicksight_data_set.total_benes_pulled_per_week_2_0.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "1f75452c-8997-4f34-b7f0-51b5832866f9.total_benes.2.1680630577824"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "total_benes"
                      data_set_identifier = aws_quicksight_data_set.total_benes_pulled_per_week_2_0.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "1f75452c-8997-4f34-b7f0-51b5832866f9.week_start.0.1654804288199"

                sort_by {
                  field {
                    direction = "DESC"
                    field_id  = "1f75452c-8997-4f34-b7f0-51b5832866f9.week_start.0.1654804288199"
                  }
                }
              }
            }
            table_options {
              cell_style {
                height = 25
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline font-size="14px">Beneficiaries Served Weekly 2.0</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "5a2bcd55-da4d-4ffd-b697-e7279e917dc1"

          chart_configuration {
            field_options {
              selected_field_options {
                field_id   = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.week_start.0.1737996153551"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.week_end.1.1737996156371"
                visibility = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Number of calls made"
                field_id     = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.until.2.1737996624893"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                rows {
                  date_dimension_field {
                    field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.week_start.0.1737996153551"

                    column {
                      column_name         = "week_start"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
                rows {
                  date_dimension_field {
                    field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.week_end.1.1737996156371"

                    column {
                      column_name         = "week_end"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
                values {
                  date_measure_field {
                    aggregation_function = "COUNT"
                    field_id             = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.until.2.1737996624893"

                    column {
                      column_name         = "until"
                      data_set_identifier = aws_quicksight_data_set.job_view.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.week_start.0.1737996153551"

                sort_by {
                  field {
                    direction = "DESC"
                    field_id  = "ebb7c5fa-ecd5-4151-b14d-366454a15efd.week_start.0.1737996153551"
                  }
                }
              }
            }
            table_options {
              row_alternate_color_options {
                status = "ENABLED"
              }
            }
            total_options {
              row_subtotal_options {
                custom_label      = "<<$aws:subtotalDimension>> Subtotal"
                totals_visibility = "HIDDEN"
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>Call Made using \"_until\"</visual-title>"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Attestation Records"
      sheet_id     = "attestation-records"

      filter_controls {
        dropdown {
          filter_control_id = "a855e4f2-51f7-438b-ad15-d44efc0b4869"
          source_filter_id  = "fe62d090-89fd-4031-891b-8c68349aa54a"
          title             = "Contract Number"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 4
              element_id   = "a855e4f2-51f7-438b-ad15-d44efc0b4869"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 3
            }
            elements {
              column_index = "4"
              column_span  = 26
              element_id   = "10d42060-0022-4309-bc5f-ae0486d21f38"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 12
            }
            elements {
              column_index = "4"
              column_span  = 26
              element_id   = "1002e8d9-e20d-4892-8c9f-6cd51cb47908"
              element_type = "VISUAL"
              row_index    = "12"
              row_span     = 12
            }
            elements {
              column_index = "4"
              column_span  = 26
              element_id   = "a4196bd0-bb81-485d-8046-6abcce9b13a0"
              element_type = "VISUAL"
              row_index    = "24"
              row_span     = 12
            }
          }
        }
      }

      visuals {
        table_visual {
          visual_id = "a4196bd0-bb81-485d-8046-6abcce9b13a0"

          chart_configuration {
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_name.1.1634062508767"

                    column {
                      column_name         = "contract_name"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_number.2.1634062508767"

                    column {
                      column_name         = "contract_number"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.attested_on.0.1634062508767"

                    column {
                      column_name         = "attested_on"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              row_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.attested_on.0.1634062508767"
                }
              }
            }
            table_options {
              orientation = "VERTICAL"
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline font-size="14px">Attestation Status</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        table_visual {
          visual_id = "1002e8d9-e20d-4892-8c9f-6cd51cb47908"

          chart_configuration {
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_name.1.1634062508767"

                    column {
                      column_name         = "contract_name"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_number.2.1634062508767"

                    column {
                      column_name         = "contract_number"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.attested_on.0.1634062508767"

                    column {
                      column_name         = "attested_on"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              row_sort {
                column_sort {
                  direction = "ASC"

                  aggregation_function {
                    categorical_aggregation_function = "COUNT"
                  }

                  sort_by {
                    column_name         = "contract_number"
                    data_set_identifier = aws_quicksight_data_set.contracts.name
                  }
                }
              }
            }
            table_options {
              orientation = "VERTICAL"
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "Not Attested"
            }
          }
        }
      }
      visuals {
        table_visual {
          visual_id = "10d42060-0022-4309-bc5f-ae0486d21f38"

          chart_configuration {
            field_options {
              order = [
                "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_number.2.1634062508767",
                "c75bf758-9a97-401e-876a-8a72ff3a6cfc.attested_on.0.1634062508767",
                "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_name.1.1634062508767",
                "c75bf758-9a97-401e-876a-8a72ff3a6cfc.hpms_parent_org_name.4.1681913503874",
                "c75bf758-9a97-401e-876a-8a72ff3a6cfc.hpms_org_marketing_name.5.1681913507162",
              ]

              selected_field_options {
                field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_number.2.1634062508767"
                width    = "117px"
              }
            }
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_number.2.1634062508767"

                    column {
                      column_name         = "contract_number"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.attested_on.0.1634062508767"

                    column {
                      column_name         = "attested_on"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.contract_name.1.1634062508767"

                    column {
                      column_name         = "contract_name"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.hpms_parent_org_name.4.1681913503874"

                    column {
                      column_name         = "hpms_parent_org_name"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.hpms_org_marketing_name.5.1681913507162"

                    column {
                      column_name         = "hpms_org_marketing_name"
                      data_set_identifier = aws_quicksight_data_set.contracts.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              row_sort {
                field_sort {
                  direction = "ASC"
                  field_id  = "c75bf758-9a97-401e-876a-8a72ff3a6cfc.hpms_parent_org_name.4.1681913503874"
                }
              }
            }
            table_options {
              orientation = "VERTICAL"

              cell_style {
                height = 25
              }

              header_style {
                height    = 25
                text_wrap = "WRAP"
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              plain_text = "Attestation Active"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Job History"
      sheet_id     = "job-history"

      filter_controls {
        date_time_picker {
          filter_control_id = "ebe3fdfc-7a15-4974-9998-23d01ff2a430"
          source_filter_id  = "42ccbac8-d73e-4fc1-a498-01d77cc5e775"
          title             = "Job Start Time"
          type              = "DATE_RANGE"
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "1ff98592-d92c-4379-b6db-5a4c39738b39"
          source_filter_id  = "c9e5cf13-418f-4f0a-b0b6-cf1cc167610f"
          title             = "Contract Number"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "1516bed3-79f6-447e-baf5-ca4403ed7ece"
          source_filter_id  = "0b802823-e9ce-4106-a77d-021fe55ccf10"
          title             = "FHIR Version"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "64b11792-3e84-4516-9ed7-6a935f82da97"
          source_filter_id  = "1609a46f-3966-4049-8d5d-1da5c33a2ed9"
          title             = "Job Status"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 6
              element_id   = "1ff98592-d92c-4379-b6db-5a4c39738b39"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 4
            }
            elements {
              column_index = "6"
              column_span  = 6
              element_id   = "1516bed3-79f6-447e-baf5-ca4403ed7ece"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 4
            }
            elements {
              column_index = "12"
              column_span  = 6
              element_id   = "ebe3fdfc-7a15-4974-9998-23d01ff2a430"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 4
            }
            elements {
              column_index = "18"
              column_span  = 6
              element_id   = "64b11792-3e84-4516-9ed7-6a935f82da97"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 4
            }
            elements {
              column_index = "0"
              column_span  = 36
              element_id   = "c77cd016-d4b9-47a4-8577-f2107e834d3b"
              element_type = "VISUAL"
              row_index    = "4"
              row_span     = 19
            }
          }
        }
      }

      visuals {
        table_visual {
          visual_id = "c77cd016-d4b9-47a4-8577-f2107e834d3b"

          chart_configuration {
            field_options {
              order = [
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.8.1666730218616",
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Contract Number.7.1666730337967",
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Data Start Date (Since Date).8.1666732789762",
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Seconds Run.7.1666730346745",
                "7b25c0a6-e388-4003-87bf-1a6f9868c1d8.9.1666732945655",
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.FHIR Version.10.1666788513892",
                "5c34508e-eb8f-45e2-aa31-c7748edd9143.10.1666733405825",
                "8186ff79-fc98-4658-90d3-4da5ee6c1d1c.11.1666801542749",
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Complete Time.9.1666733359527",
                "dd7d5f30-dc2c-40d1-9e1d-901304d789eb.11.1666788294165",
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.since.11.1737592923089",
                "30ce4efe-3e95-486c-baf3-dcaa4bedafff.# EoBs Written.7.1666730688662",
              ]

              selected_field_options {
                field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.8.1666730218616"
                width    = "130px"
              }
              selected_field_options {
                field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Complete Time.9.1666733359527"
                width    = "176px"
              }
            }
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  date_dimension_field {
                    date_granularity = "HOUR"
                    field_id         = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.8.1666730218616"

                    column {
                      column_name         = "Job Start Time"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }

                    format_configuration {
                      date_time_format = "MM-DD-YYYY h:mm a"

                      null_value_format_configuration {
                        null_string = "null"
                      }
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Contract Number.7.1666730337967"

                    column {
                      column_name         = "Contract Number"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Data Start Date (Since Date).8.1666732789762"

                    column {
                      column_name         = "Data Start Date (Since Date)"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Seconds Run.7.1666730346745"

                    column {
                      column_name         = "Seconds Run"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  numerical_dimension_field {
                    field_id = "7b25c0a6-e388-4003-87bf-1a6f9868c1d8.9.1666732945655"

                    column {
                      column_name         = "# Days Searched"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.FHIR Version.10.1666788513892"

                    column {
                      column_name         = "FHIR Version"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  numerical_dimension_field {
                    field_id = "5c34508e-eb8f-45e2-aa31-c7748edd9143.10.1666733405825"

                    column {
                      column_name         = "Average Time Per Bene (ms)"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    date_granularity = "SECOND"
                    field_id         = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Complete Time.9.1666733359527"

                    column {
                      column_name         = "Job Complete Time"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  numerical_dimension_field {
                    field_id = "dd7d5f30-dc2c-40d1-9e1d-901304d789eb.11.1666788294165"

                    column {
                      column_name         = "Average Time Per EoB (ms)"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.since.11.1737592923089"

                    column {
                      column_name         = "since"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "8186ff79-fc98-4658-90d3-4da5ee6c1d1c.11.1666801542749"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "# Benes Searched"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.# EoBs Written.7.1666730688662"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "# EoBs Written"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
              }
            }
            sort_configuration {
              row_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.8.1666730218616"
                }
              }
            }
            table_options {
              cell_style {
                height                    = 25
                horizontal_text_alignment = "LEFT"

                border {
                  uniform_border {
                    color     = "#AAAAAA"
                    style     = "SOLID"
                    thickness = 1
                  }
                }

                font_configuration {
                  font_size {
                    relative = "MEDIUM"
                  }
                }
              }
              header_style {
                background_color          = "#02075D"
                height                    = 28
                horizontal_text_alignment = "LEFT"
                text_wrap                 = "WRAP"
                vertical_text_alignment   = "TOP"

                border {
                  uniform_border {
                    color     = "#000000"
                    style     = "SOLID"
                    thickness = 1
                  }
                }

                font_configuration {
                  font_color = "#EEEEEE"
                }
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline color="#000000">AB2D Job History</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Avg. Bene/EoB Time"
      sheet_id     = "avg-bene-eob-time"

      filter_controls {
        relative_date_time {
          filter_control_id = "35d8f0b7-0ef0-498a-8692-917b6752579e"
          source_filter_id  = "9d0f751d-9064-47bf-b67c-d56b87d0847c"
          title             = "Job Start Time"
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "70cc9e7d-c440-4086-85b9-f27ed32fc98c"
          source_filter_id  = "03d98245-021d-407f-98b6-d5862e0aab45"
          title             = "FHIR Version"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
          }
        }
      }
      filter_controls {
        slider {
          filter_control_id = "b4007d0f-98fe-4d16-8cd0-8699c69c4ec9"
          maximum_value     = 1000
          minimum_value     = 1
          source_filter_id  = "25cf1e9c-0e9d-4f7f-bbab-65eab5834615"
          step_size         = 30
          title             = "Minimun # Days Searched"
          type              = "SINGLE_POINT"

          display_options {
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 7
              element_id   = "70cc9e7d-c440-4086-85b9-f27ed32fc98c"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "7"
              column_span  = 9
              element_id   = "35d8f0b7-0ef0-498a-8692-917b6752579e"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "16"
              column_span  = 8
              element_id   = "b4007d0f-98fe-4d16-8cd0-8699c69c4ec9"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 5
            }
            elements {
              column_index = "0"
              column_span  = 18
              element_id   = "d26bd84e-0bc7-48d7-96f0-7172075eef9c"
              element_type = "VISUAL"
              row_index    = "5"
              row_span     = 12
            }
            elements {
              column_index = "18"
              column_span  = 18
              element_id   = "fa95218c-a158-49bd-9f1b-0734c06674d6"
              element_type = "VISUAL"
              row_index    = "5"
              row_span     = 12
            }
            elements {
              column_index = "0"
              column_span  = 18
              element_id   = "dd13a388-dff4-44be-bba3-0de892971410"
              element_type = "VISUAL"
              row_index    = "17"
              row_span     = 12
            }
            elements {
              column_index = "18"
              column_span  = 18
              element_id   = "f7f17091-572a-49ff-beb6-5d037a0cfb00"
              element_type = "VISUAL"
              row_index    = "17"
              row_span     = 12
            }
          }
        }
      }

      visuals {
        line_chart_visual {
          visual_id = "dd13a388-dff4-44be-bba3-0de892971410"

          chart_configuration {
            type = "LINE"

            data_labels {
              overlap    = "DISABLE_OVERLAP"
              visibility = "HIDDEN"
            }

            field_wells {
              line_chart_aggregated_field_wells {
                category {
                  date_dimension_field {
                    date_granularity = "MONTH"
                    field_id         = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666789482439"
                    hierarchy_id     = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666789482439"

                    column {
                      column_name         = "Job Start Time"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "5c34508e-eb8f-45e2-aa31-c7748edd9143.2.1666789255409"

                    aggregation_function {
                      simple_numerical_aggregation = "AVERAGE"
                    }

                    column {
                      column_name         = "Average Time Per Bene (ms)"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.# Bene Searched.2.1722965108573"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "# Bene Searched"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
              }
            }

            primary_y_axis_label_options {
              axis_label_options {
                custom_label = "Time Per Bene (ms)"

                apply_to {
                  field_id = "5c34508e-eb8f-45e2-aa31-c7748edd9143.2.1666789255409"

                  column {
                    column_name         = "Average Time Per Bene (ms)"
                    data_set_identifier = aws_quicksight_data_set.benes_searched.name
                  }
                }
              }
            }

            sort_configuration {
              category_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666789482439"
                }
              }
            }

            tooltip {
              selected_tooltip_type = "DETAILED"
              tooltip_visibility    = "VISIBLE"

              field_base_tooltip {
                aggregation_visibility = "HIDDEN"
                tooltip_title_type     = "PRIMARY_VALUE"

                tooltip_fields {
                  column_tooltip_item {
                    visibility = "VISIBLE"

                    aggregation {
                      date_aggregation_function = "MIN"
                    }

                    column {
                      column_name         = "Job Start Time"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "5c34508e-eb8f-45e2-aa31-c7748edd9143.2.1666789255409"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666789482439"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.# Bene Searched.2.1722965108573"
                    visibility = "VISIBLE"
                  }
                }
              }
            }
          }

          column_hierarchies {
            date_time_hierarchy {
              hierarchy_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666789482439"
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline color="#000000" font-size="14px">Monthly Avg Time Per Bene (ms)</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        scatter_plot_visual {
          visual_id = "d26bd84e-0bc7-48d7-96f0-7172075eef9c"

          chart_configuration {
            data_labels {
              overlap    = "DISABLE_OVERLAP"
              visibility = "HIDDEN"
            }
            field_wells {
              scatter_plot_unaggregated_field_wells {
                x_axis {
                  numerical_dimension_field {
                    field_id = "8186ff79-fc98-4658-90d3-4da5ee6c1d1c.1.1666802119560"

                    column {
                      column_name         = "# Benes Searched"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                y_axis {
                  numerical_dimension_field {
                    field_id = "5c34508e-eb8f-45e2-aa31-c7748edd9143.1.1666788757840"

                    column {
                      column_name         = "Average Time Per Bene (ms)"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
              }
            }
            tooltip {
              selected_tooltip_type = "DETAILED"
              tooltip_visibility    = "VISIBLE"

              field_base_tooltip {
                aggregation_visibility = "HIDDEN"
                tooltip_title_type     = "PRIMARY_VALUE"

                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "5c34508e-eb8f-45e2-aa31-c7748edd9143.1.1666788757840"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  column_tooltip_item {
                    visibility = "VISIBLE"

                    aggregation {
                      date_aggregation_function = "MIN"
                    }

                    column {
                      column_name         = "Job Start Time"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "8186ff79-fc98-4658-90d3-4da5ee6c1d1c.1.1666802119560"
                    visibility = "VISIBLE"
                  }
                }
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline color="#000000" font-size="14px">Avg. Time Per Bene (ms) in relation to # Benes Searched</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        scatter_plot_visual {
          visual_id = "fa95218c-a158-49bd-9f1b-0734c06674d6"

          chart_configuration {
            data_labels {
              overlap    = "DISABLE_OVERLAP"
              visibility = "HIDDEN"
            }
            field_wells {
              scatter_plot_unaggregated_field_wells {
                x_axis {
                  numerical_dimension_field {
                    field_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.# EoBs Written.1.1666789457239"

                    column {
                      column_name         = "# EoBs Written"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                y_axis {
                  numerical_dimension_field {
                    field_id = "dd7d5f30-dc2c-40d1-9e1d-901304d789eb.1.1666788801627"

                    column {
                      column_name         = "Average Time Per EoB (ms)"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
              }
            }
            tooltip {
              selected_tooltip_type = "DETAILED"
              tooltip_visibility    = "VISIBLE"

              field_base_tooltip {
                aggregation_visibility = "HIDDEN"
                tooltip_title_type     = "PRIMARY_VALUE"

                tooltip_fields {
                  column_tooltip_item {
                    visibility = "VISIBLE"

                    aggregation {
                      date_aggregation_function = "MIN"
                    }

                    column {
                      column_name         = "Job Start Time"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "dd7d5f30-dc2c-40d1-9e1d-901304d789eb.1.1666788801627"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.# EoBs Written.1.1666789457239"
                    visibility = "VISIBLE"
                  }
                }
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline color="#000000" font-size="14px">Avg. Time Per EoB (ms) in relation to # EoBs Written</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
      visuals {
        line_chart_visual {
          visual_id = "f7f17091-572a-49ff-beb6-5d037a0cfb00"

          chart_configuration {
            type = "LINE"

            data_labels {
              overlap    = "DISABLE_OVERLAP"
              visibility = "HIDDEN"
            }

            field_wells {
              line_chart_aggregated_field_wells {
                category {
                  date_dimension_field {
                    date_granularity = "MONTH"
                    field_id         = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666790737470"
                    hierarchy_id     = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666790737470"

                    column {
                      column_name         = "Job Start Time"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "dd7d5f30-dc2c-40d1-9e1d-901304d789eb.1.1666790747296"

                    aggregation_function {
                      simple_numerical_aggregation = "AVERAGE"
                    }

                    column {
                      column_name         = "Average Time Per EoB (ms)"
                      data_set_identifier = aws_quicksight_data_set.benes_searched.name
                    }
                  }
                }
              }
            }

            primary_y_axis_label_options {
              axis_label_options {
                custom_label = "Time Per EoB (ms)"

                apply_to {
                  field_id = "dd7d5f30-dc2c-40d1-9e1d-901304d789eb.1.1666790747296"

                  column {
                    column_name         = "Average Time Per EoB (ms)"
                    data_set_identifier = aws_quicksight_data_set.benes_searched.name
                  }
                }
              }
            }

            sort_configuration {
              category_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666790737470"
                }
              }
            }

            tooltip {
              selected_tooltip_type = "DETAILED"
              tooltip_visibility    = "VISIBLE"

              field_base_tooltip {
                aggregation_visibility = "HIDDEN"
                tooltip_title_type     = "PRIMARY_VALUE"

                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666790737470"
                    visibility = "VISIBLE"
                  }
                }
                tooltip_fields {
                  field_tooltip_item {
                    field_id   = "dd7d5f30-dc2c-40d1-9e1d-901304d789eb.1.1666790747296"
                    visibility = "VISIBLE"
                  }
                }
              }
            }
          }

          column_hierarchies {
            date_time_hierarchy {
              hierarchy_id = "30ce4efe-3e95-486c-baf3-dcaa4bedafff.Job Start Time.1.1666790737470"
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = <<-EOT
                                <visual-title>
                                  <inline color="#000000" font-size="14px">Monthly Avg Time Per EoB (ms)</inline>
                                </visual-title>
                            EOT
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Coverage Counts"
      sheet_id     = "coverage-counts"

      layouts {
        configuration {
          grid_layout {
            canvas_size_options {
              screen_canvas_size_options {
                resize_option = "RESPONSIVE"
              }
            }
            elements {
              column_index = "1"
              column_span  = 34
              element_id   = "d01b9194-fd61-4ba2-a8ef-957a0b49c62d"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 16
            }
          }
        }
      }

      visuals {
        pivot_table_visual {
          visual_id = "d01b9194-fd61-4ba2-a8ef-957a0b49c62d"

          chart_configuration {
            field_options {
              selected_field_options {
                field_id   = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.contract_number.0.1677007953888"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.month.3.1681913145386"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.ab2d.5.1677008304496"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.bfd.4.1681913150987"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.hpms.5.1681913154953"
                visibility = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                rows {
                  categorical_dimension_field {
                    field_id = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.contract_number.0.1677007953888"

                    column {
                      column_name         = "contract_number"
                      data_set_identifier = aws_quicksight_data_set.coverage_counts.name
                    }
                  }
                }
                rows {
                  numerical_dimension_field {
                    field_id = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.month.3.1681913145386"

                    column {
                      column_name         = "month"
                      data_set_identifier = aws_quicksight_data_set.coverage_counts.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.ab2d.5.1677008304496"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "ab2d"
                      data_set_identifier = aws_quicksight_data_set.coverage_counts.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.bfd.4.1681913150987"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "bfd"
                      data_set_identifier = aws_quicksight_data_set.coverage_counts.name
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "bc6ff924-19e3-4405-aea1-fccfd3738ce3.hpms.5.1681913154953"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "hpms"
                      data_set_identifier = aws_quicksight_data_set.coverage_counts.name
                    }
                  }
                }
              }
            }
            table_options {
              cell_style {
                height = 25
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>AB2D, BFD, and HPMS coverage counts</visual-title>"
            }
          }
        }
      }
    }
  }

  dynamic "permissions" {
    for_each = local.data_admins
    content {
      actions = [
        "quicksight:DeleteAnalysis",
        "quicksight:DescribeAnalysis",
        "quicksight:DescribeAnalysisPermissions",
        "quicksight:QueryAnalysis",
        "quicksight:RestoreAnalysis",
        "quicksight:UpdateAnalysis",
        "quicksight:UpdateAnalysisPermissions",
      ]
      principal = "arn:aws:quicksight:us-east-1:${local.aws_account_id}:${permissions.value}"
    }
  }
}
