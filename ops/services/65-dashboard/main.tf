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

  custom_widgets = [
    {
      type         = "timeseries"
      title        = "API Request Rate by Endpoint"
      query        = "sum:ab2d.api.request.count{$env} by {endpoint}.as_rate()"
      display_type = "line"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "API Request Rate by Status Code"
      query        = "sum:ab2d.api.request.count{$env} by {status_code}.as_rate()"
      display_type = "bars"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "API Request Duration median / p95 / max (ms)"
      query        = "avg:ab2d.api.request.duration.median{$env}, avg:ab2d.api.request.duration.95percentile{$env}, max:ab2d.api.request.duration.max{$env}"
      display_type = "line"
      precision    = 0
    },
    {
      type         = "toplist"
      title        = "API Request Duration p95 by Endpoint (ms)"
      query        = "avg:ab2d.api.request.duration.95percentile{$env} by {endpoint}"
      display_type = "line"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "API Errors by Status Class (4xx vs 5xx)"
      query        = "sum:ab2d.api.error.count{$env} by {status_class}.as_count()"
      display_type = "bars"
      precision    = 0
    },
    {
      type         = "toplist"
      title        = "API Errors by Error Type"
      query        = "sum:ab2d.api.error.count{$env} by {error_type}.as_count()"
      display_type = "bars"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "API Error Rate (%)"
      query        = "sum:ab2d.api.error.count{$env}.as_count() / sum:ab2d.api.request.count{$env}.as_count() * 100"
      display_type = "line"
      precision    = 2
    },
    {
      type         = "toplist"
      title        = "API Requests by Client Version"
      query        = "sum:ab2d.api.client.version{$env} by {client_version}.as_count()"
      display_type = "bars"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "API Requests by API Version"
      query        = "sum:ab2d.api.client.version{$env} by {api_version}.as_rate()"
      display_type = "line"
      precision    = 0
    },
    {
      type         = "timeseries"
      title        = "API Request / Response Payload Size p95 (bytes)"
      query        = "avg:ab2d.api.request.size.95percentile{$env}, avg:ab2d.api.response.size.95percentile{$env}"
      display_type = "line"
      precision    = 0
    },
    {
      type         = "query_value"
      title        = "API Requests"
      query        = "sum:ab2d.api.request.count{$env}.as_count()"
      display_type = "line"
      precision    = 0
    },
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
