terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

locals {
  default_tags   = module.platform.default_tags
  env            = terraform.workspace
  image_repo_uri = data.aws_ecr_repository.idr_db_importer.repository_url
  service        = "idr-db-importer"

  ssm_root_map = {
    this = "/ab2d/${local.env}/idr-db-importer/"
  }
}

module "platform" {
  source    = "github.com/CMSgov/cdap//terraform/modules/platform?ref=8a6527c0689bb46ae0e74bd47e4087ab59cff1b0"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = local.app
  env          = local.env
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-idr-db-importer"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}


module "service" {
  source = "github.com/CMSgov/cdap//terraform/modules/service?ref=52af0763fab4e65b29ead8bf88774f0bad4bdd87"

  cluster_arn          = data.aws_ecs_cluster.shared.arn
  cpu                  = 1024
  memory               = 2048
  desired_count        = 0
  enable_datadog_agent = true
  image                = "${local.image_repo_uri}:${var.image_tag}"
  platform             = module.platform
  subnets              = keys(module.platform.private_subnets)

  additional_task_role_policies = { idr_db_importer_bucket_write_and_read = aws_iam_policy.idr_db_importer_bucket_write_and_read.arn }

  container_environment = concat(
    [
      { name = "AB2D_DB_DATABASE", value = data.aws_ssm_parameter.ab2d_db_database.value },
      { name = "AB2D_DB_HOST", value = data.aws_ssm_parameter.ab2d_db_host.value },
      { name = "AB2D_DB_PORT", value = "5432" },
      { name = "S3_BUCKET", value = module.idr_db_importer_bucket.id },
      { name = "ENVIRONMENT", value = local.env }
    ],
    module.platform.parent_env == "prod" ? [
      { name = "IDR_SNOWFLAKE_URL", value = "jdbc:snowflake://cms-idr.privatelink.snowflakecomputing.com" },
      { name = "IDR_SNOWFLAKE_DB", value = "IDRC_PRD" },
      { name = "IDR_SNOWFLAKE_SCHEMA", value = "CMS_VDM_VIEW_MDCR_PRD" }
    ] : []
  )

  container_secrets = concat(
    [
      { name = "AB2D_DB_PASSWORD", valueFrom = data.aws_ssm_parameter.ab2d_db_password.arn },
      { name = "AB2D_DB_USER", valueFrom = data.aws_ssm_parameter.ab2d_db_user.arn }
    ],
    module.platform.parent_env == "prod" ? [
      { name = "IDR_SNOWFLAKE_PRIVATE_KEY", valueFrom = data.aws_ssm_parameter.snowflake_private_key[0].arn },
      { name = "IDR_SNOWFLAKE_WAREHOUSE", valueFrom = data.aws_ssm_parameter.snowflake_warehouse[0].arn },
      { name = "IDR_SNOWFLAKE_USER", valueFrom = data.aws_ssm_parameter.snowflake_user[0].arn },
      { name = "IDR_SNOWFLAKE_ROLE", valueFrom = data.aws_ssm_parameter.snowflake_role[0].arn }
    ] : []
  )
}

module "idr_db_importer_bucket" {
  source = "github.com/CMSgov/cdap//terraform/modules/bucket?ref=7c070cd2e8c6b1407961c35976553446df8fafd3"

  additional_bucket_policies = [data.aws_iam_policy_document.idr_db_importer_bucket_extended_deny_policy.json]
  app                        = module.platform.app
  env                        = local.parent_env
  name                       = "${module.platform.app}-${module.platform.env}-idr-db-importer"
  ssm_parameter              = "/ab2d/${module.platform.env}/core/nonsensitive/idr-db-importer-bucket"
}


resource "aws_scheduler_schedule" "idr_db_importer" {
  group_name          = "default"
  name                = "${local.service_prefix}-idr-db-importer-eventbridge-schedule"
  schedule_expression = "cron(0 11 ? * MON-SAT *)" # Every day at 11am UTC except Sunday

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = data.aws_ecs_cluster.shared.arn
    role_arn = aws_iam_role.idr_db_importer_eventbridge_scheduler.arn

    ecs_parameters {
      launch_type = "FARGATE"

      task_definition_arn = trimsuffix(
        module.service.task_definition.arn, ":${module.service.task_definition.revision}"
      )

      network_configuration {
        assign_public_ip = false
        security_groups  = [module.service.task_security_group_id]
        subnets          = keys(module.platform.private_subnets)
      }
    }
  }
}
