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
    datadog  = "/cdap/${local.env}/datadog/cicd/"
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
}

###################
# Common Monitors #
###################

module "common_datadog_monitors" {
  source = "github.com/CMSgov/cdap//terraform/modules/datadog_monitors?ref=PLT-1655/notify"

  app            = "ab2d"
  env            = local.env
  monitor_config = local.monitor_config
}
