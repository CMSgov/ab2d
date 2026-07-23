terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~>6"
    }
    datadog = {
      source  = "DataDog/datadog"
      version = "~>4.4"
    }
  }
}

# Leverage per app- API and application keys that are managed by CDAP in services/datadog-cicd-keys
provider "datadog" {
  api_key = sensitive(module.platform.ssm.datadog.api_key.value)
  app_key = sensitive(module.platform.ssm.datadog.application_key.value)
  api_url = "https://api.ddog-gov.com"
}

module "platform" {
  source    = "github.com/CMSgov/cdap//terraform/modules/platform?ref=941672f97adfd8a19ce6533313302c4c74bac7a8"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = local.app
  env          = local.env
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/65-dashboard"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "dashboard"


  ssm_root_map = {
    common   = "/ab2d/${local.env}/common"
    core     = "/ab2d/${local.env}/core"
    accounts = "/ab2d/mgmt/aws-account-numbers"
    splunk   = "/ab2d/mgmt/splunk"
    datadog  = "/cdap/${local.env}/datadog/cicd/"
  }
}

module "datadog_dashboard" {
  source = "github.com/CMSgov/cdap//terraform/modules/datadog_dashboard?ref=945fbd644cc8d239bdf3f3a3a7241fb6066a0f55"

  app = local.app

  enable_default_widgets = {
    ecs    = true
    alb    = true
    aurora = true
    sns    = true
    sqs    = true
    lambda = true
    s3     = true
    apm    = true
  }

  widget_live_spans = {
    ecs    = "4h"
    alb    = "4h"
    aurora = "4h"
    sns    = "4h"
    sqs    = "4h"
    lambda = "1d"
    s3     = "1w"
    apm    = "1h"
  }

  # Coverage V3 import row counts and deltas, sourced from the DogStatsD custom metrics emitted by
  # CoverageV3SyncMetrics (ab2d.coverage.v3.*). These surface the same before/after/delta counts that
  # are written to the v3.coverage_v3_audit table so the team can confirm an import updated data
  # without inspecting logs or querying the database. Queries use the dashboard-wide $env template
  # variable (prefix "environment"), which matches the environment tag on these metrics.
  custom_widgets = [
    {
      type         = "timeseries"
      title        = "Coverage V3 Import - Recent Table Rows (staged / before / after)"
      query        = "sum:ab2d.coverage.v3.import.rows_staged{$env}, sum:ab2d.coverage.v3.import.rows_before{$env}, sum:ab2d.coverage.v3.import.rows_after{$env}"
      display_type = "line"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "Coverage V3 Import - Recent Table Rows Delta (after - before)"
      query        = "sum:ab2d.coverage.v3.import.rows_delta{$env}"
      display_type = "bars"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "Coverage V3 Historical - Rows Moved vs Deleted"
      query        = "sum:ab2d.coverage.v3.historical.rows_moved{$env}, sum:ab2d.coverage.v3.historical.rows_deleted{$env}"
      display_type = "bars"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "Coverage V3 Sync Completions by Result"
      query        = "sum:ab2d.coverage.v3.import.completed{$env} by {result}.as_count()"
      display_type = "bars"
      precision    = 0
    },
    {
      type         = "query_value"
      title        = "Coverage V3 Import - Total Rows Delta"
      query        = "sum:ab2d.coverage.v3.import.rows_delta{$env}"
      display_type = "line"
      precision    = 0
    },
    {
      type         = "toplist"
      title        = "Coverage V3 Import - Rows Delta by Contract"
      query        = "sum:ab2d.coverage.v3.import.rows_delta{$env} by {contract}"
      display_type = "line"
      precision    = 0
    },
  ]
  runbook_url = "https://definerunbook.cdap.internal.cms.gov" #FIXME to provide an actual runbook
}
