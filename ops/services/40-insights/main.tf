terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = local.app
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/40-insights"
  service     = local.service
  ssm_root_map = {
    core     = "/ab2d/${local.env}/core"
    insights = "/ab2d/${local.env}/insights"
  }
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "insights"

  db_name            = module.platform.ssm.core.database_name.value
  db_password        = module.platform.ssm.core.database_password.value
  db_username        = module.platform.ssm.core.database_user.value
  vpc_id             = module.platform.vpc_id
  private_subnet_ids = nonsensitive(keys(module.platform.private_subnets))
  analysis_data_sets = toset([
    aws_quicksight_data_set.ab2d_statistics,
    aws_quicksight_data_set.benes_searched,
    aws_quicksight_data_set.contracts,
    aws_quicksight_data_set.contracts_one_job_minimum,
    aws_quicksight_data_set.coverage_counts,
    aws_quicksight_data_set.eob_search_summaries_1,
    aws_quicksight_data_set.eob_search_summaries_2,
    aws_quicksight_data_set.eob_search_summaries_event,
    aws_quicksight_data_set.job_view,
    aws_quicksight_data_set.total_benes_pulled_per_week_2_0,
    aws_quicksight_data_set.uptime,
  ])
  aws_account_id     = module.platform.aws_caller_identity.account_id
  non_test_contracts = yamldecode(file("${path.root}/non-test-contracts.yml"))
  data_admins        = toset(yamldecode(nonsensitive(module.platform.ssm.insights.administrators_yaml.value)))
}

resource "aws_quicksight_template" "a" {
  template_id         = "${local.service_prefix}-dasg-metrics"
  name                = "AB2D DASG Metrics"
  version_description = "foo"
  source_entity {
    source_analysis {
      arn = aws_quicksight_analysis.a.arn
      dynamic "data_set_references" {
        for_each = local.analysis_data_sets
        content {
          data_set_arn         = data_set_references.value.arn
          data_set_placeholder = data_set_references.value.name
        }
      }
    }
  }
}

resource "aws_quicksight_dashboard" "a" {
  dashboard_id        = "${local.service_prefix}-dasg-metrics"
  name                = "AB2D DASG Metrics"
  version_description = "Attempted update of calls made column in _until usage metric"
  source_entity {
    source_template {
      arn = aws_quicksight_template.a.arn
      dynamic "data_set_references" {
        for_each = local.analysis_data_sets
        content {
          data_set_arn         = data_set_references.value.arn
          data_set_placeholder = data_set_references.value.name
        }
      }
    }
  }

  dynamic "permissions" {
    for_each = local.data_admins
    content {
      actions = [
        "quicksight:DeleteDashboard",
        "quicksight:DescribeDashboard",
        "quicksight:DescribeDashboardPermissions",
        "quicksight:ListDashboardVersions",
        "quicksight:QueryDashboard",
        "quicksight:UpdateDashboard",
        "quicksight:UpdateDashboardPermissions",
        "quicksight:UpdateDashboardPublishedVersion",
      ]
      principal = "arn:aws:quicksight:us-east-1:${local.aws_account_id}:${permissions.value}"
    }
  }
}
