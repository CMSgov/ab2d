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
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/60-monitors"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "monitors"


  ssm_root_map = {
    common   = "/ab2d/${local.env}/common"
    core     = "/ab2d/${local.env}/core"
    accounts = "/ab2d/mgmt/aws-account-numbers"
    splunk   = "/ab2d/mgmt/splunk"
    datadog  = "/ab2d/${local.env}/datadog/cicd/"
  }

  defaults   = yamldecode(file("config/defaults.yml"))
  env_config = yamldecode(file("config/${local.env}.yml"))

  monitor_config = {
    for key in distinct(concat(keys(local.defaults), keys(local.env_config))) :
    key => try(
      # Attempt map merge (works if both values are map/object-typed)
      merge(
        lookup(local.defaults, key, {}),
        lookup(local.env_config, key, {})
      ),
      # Fallback to scalar: env wins, then default
      lookup(local.env_config, key, lookup(local.defaults, key, null))
    )
  }

  coverage_v3_env_tag = lookup({
    dev     = "ab2d-dev"
    test    = "ab2d-east-impl"
    sandbox = "ab2d-sbx-sandbox"
    prod    = "ab2d-east-prod"
  }, local.env, local.env)

  # Alerts for suspicious Coverage V3 import behavior
  coverage_v3_monitors = [
    {
      name    = "AB2D Coverage V3 - Import staged zero rows in 24h (${local.env})"
      type    = "metric alert"
      message = "No Coverage V3 rows were staged into the recent coverage table in the last 24h for ${local.env}. The IDR import or staging sync may have stalled, or completed without updating coverage data."
      query   = "sum(last_1d):sum:ab2d.coverage.v3.import.rows_staged{environment:${local.coverage_v3_env_tag}} <= 0"
      thresholds = {
        critical = 0
      }
      notify_no_data            = true
      no_data_timeframe_minutes = 1500
      tags                      = ["service:coverage", "feature:coverage-v3-import"]
    },
    {
      name    = "AB2D Coverage V3 - Import row delta anomaly (${local.env})"
      type    = "query alert"
      message = "The Coverage V3 import row delta for ${local.env} is anomalous (unusually high or low) compared to its historical baseline."
      query   = "avg(last_4h):anomalies(sum:ab2d.coverage.v3.import.rows_delta{environment:${local.coverage_v3_env_tag}}, 'agile', 3) >= 1"
      thresholds = {
        critical = 1
      }
      notify_no_data            = false
      no_data_timeframe_minutes = 1500
      tags                      = ["service:coverage", "feature:coverage-v3-import"]
    },
    {
      name    = "AB2D Coverage V3 - Sync failures detected (${local.env})"
      type    = "metric alert"
      message = "One or more Coverage V3 staging syncs reported SYNC_FAILED_FOR_CONTRACT in the last 24h for ${local.env} (row-count mismatch during the staging copy). Coverage data may be inconsistent for the affected contract(s)."
      query   = "sum(last_1d):sum:ab2d.coverage.v3.import.completed{environment:${local.coverage_v3_env_tag},result:sync_failed_for_contract}.as_count() > 0"
      thresholds = {
        critical = 0
      }
      notify_no_data            = false
      no_data_timeframe_minutes = 1500
      tags                      = ["service:coverage", "feature:coverage-v3-import"]
    },
  ]
}

###################
# Common Monitors #
###################

module "common_datadog_monitors" {
  source = "github.com/CMSgov/cdap//terraform/modules/datadog_monitors?ref=945fbd644cc8d239bdf3f3a3a7241fb6066a0f55"

  app             = "ab2d"
  env             = local.env
  monitor_config  = local.monitor_config
  custom_monitors = local.coverage_v3_monitors
}
