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
  source    = "github.com/CMSgov/cdap//terraform/modules/platform?ref=f6fe4544d0d6ed72c50605261f0c3091487753e1"
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

  coverage_v3_base_tags = [
    "application:${local.app}",
    "environment:${local.env}",
    "managed-by:tofu",
    local.monitor_config.shadow_mode ? "shadow-mode:true" : "shadow-mode:false",
  ]

  # Readable alert time for the Slack message. The CDAP slack webhook's Date field is $DATE (epoch
  # milliseconds), which the Slack workflow prints verbatim; Datadog has no human-readable date
  # variable for webhooks, and Workflow Builder does not render <!date^...> tokens.
  alert_time = "Triggered at {{local_time 'last_triggered_at' 'UTC'}} (UTC)."

  coverage_v3_import_eval_hour   = 23
  coverage_v3_import_eval_minute = 45

  # Alerts for suspicious Coverage V3 import behavior
  coverage_v3_monitors = [
    {
      name    = "AB2D Coverage V3 - Import row delta anomaly (${local.env})"
      type    = "query alert"
      message = "The Coverage V3 staging copy for contract {{contract.name}} in ${local.env} removed more than 5% of its recent coverage rows ({{value}}% change) in the last 24h. The IDR extract for that contract is probably incomplete; check the idr-db-importer ECS task for the day and the worker's copyFromStagingTablesToRecentForAllContracts log for the contract."
      query   = "min(last_1d):sum:ab2d.coverage.v3.import.rows_delta{environment:${local.coverage_v3_env_tag}} by {contract} / sum:ab2d.coverage.v3.import.rows_before{environment:${local.coverage_v3_env_tag}} by {contract} * 100 < -5"
      thresholds = {
        critical = -5
      }
      on_missing_data = "resolve"
      tags            = ["service:coverage", "feature:coverage-v3-import"]
    },
    {
      name    = "AB2D Coverage V3 - Sync failures detected (${local.env})"
      type    = "metric alert"
      message = "One or more Coverage V3 staging syncs reported SYNC_FAILED_FOR_CONTRACT in the last 24h for ${local.env} (row-count mismatch during the staging copy). Coverage data may be inconsistent for the affected contract(s)."
      query   = "sum(last_1d):sum:ab2d.coverage.v3.import.completed{environment:${local.coverage_v3_env_tag},result:sync_failed_for_contract}.as_count() > 0"
      thresholds = {
        critical = 0
      }
      tags = ["service:coverage", "feature:coverage-v3-import"]
    },
  ]
}

###################
# Common Monitors #
###################

module "common_datadog_monitors" {
  source = "github.com/CMSgov/cdap//terraform/modules/datadog_monitors?ref=f6fe4544d0d6ed72c50605261f0c3091487753e1"

  app            = "ab2d"
  env            = local.env
  monitor_config = local.monitor_config
  custom_monitors = [
    for m in concat(local.coverage_v3_monitors, local.ecs_monitors) :
    merge(m, { message = "${m.message} ${local.alert_time}" })
  ]
}

##############################
# Coverage V3 Import Monitor #
##############################
resource "datadog_monitor" "coverage_v3_import_staged_zero_rows" {
  name = "AB2D Coverage V3 - Import staged no rows on a scheduled import day (${local.env})"
  type = "query alert"

  message = join(" ", [
    "No Coverage V3 rows were staged into the recent coverage table on a day the IDR importer was",
    "scheduled to run (Mon-Sat) for ${local.env}. The IDR import or the staging sync may have",
    "stalled, or completed without updating coverage data. This is evaluated once per scheduled",
    "import day at ${format("%02d:%02d", local.coverage_v3_import_eval_hour, local.coverage_v3_import_eval_minute)} UTC over that UTC calendar day only, so it is not",
    "satisfied by the previous day's import. Start with the idr-db-importer ECS task for the day",
    "and then the worker's copyFromStagingTablesToRecentForAllContracts run.",
    local.alert_time,
    module.common_datadog_monitors.notify,
  ])

  query = "sum(current_1d):sum:ab2d.coverage.v3.import.rows_staged{environment:${local.coverage_v3_env_tag}} <= 0"

  monitor_thresholds {
    critical = 0
  }

  on_missing_data = "show_and_notify_no_data"

  require_full_window = false
  include_tags        = true

  scheduling_options {
    evaluation_window {
      day_starts = "00:00"
      timezone   = "UTC"
    }

    custom_schedule {
      recurrence {
        rrule    = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA;BYHOUR=${local.coverage_v3_import_eval_hour};BYMINUTE=${local.coverage_v3_import_eval_minute}"
        timezone = "UTC"
      }
    }
  }

  tags = concat(local.coverage_v3_base_tags, ["service:coverage", "feature:coverage-v3-import"])
}
