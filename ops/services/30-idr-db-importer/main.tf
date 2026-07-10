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
}

module "platform" {
  source    = "github.com/CMSgov/cdap//terraform/modules/platform?ref=8a6527c0689bb46ae0e74bd47e4087ab59cff1b0"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = local.app
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-idr-db-importer"
  service     = local.service
}


module "service" {
  source = "github.com/CMSgov/cdap//terraform/modules/service?ref=52af0763fab4e65b29ead8bf88774f0bad4bdd87"

  cluster_arn          = data.aws_ecs_cluster.shared.arn
  cpu                  = 1024
  memory               = 2048
  desired_count        = 0
  enable_datadog_agent = false
  execution_role_arn   = data.aws_iam_role.idr_db_importer_task_execution.arn
  image                = "${local.image_repo_uri}:${var.image_tag}"
  platform             = module.platform
  security_groups      = [data.aws_security_group.idr_db_importer.id]
  subnets              = keys(module.platform.private_subnets)

  additional_task_role_policies = { s3 = aws_iam_policy.idr_db_importer_task.arn }

  container_environment = concat(
    [
      { name = "AB2D_DB_DATABASE", value = data.aws_ssm_parameter.ab2d_db_database.value },
      { name = "AB2D_DB_HOST", value = data.aws_ssm_parameter.ab2d_db_host.value },
      { name = "AB2D_DB_PORT", value = "5432" },
      { name = "S3_BUCKET", value = data.aws_ssm_parameter.idr_db_importer_bucket.value },
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
      { name = "IDR_SNOWFLAKE_PRIVATE_KEY", valueFrom = data.aws_ssm_parameter.idr_private_key[0].arn },
      { name = "IDR_SNOWFLAKE_WAREHOUSE", valueFrom = data.aws_ssm_parameter.idr_snowflake_warehouse[0].arn },
      { name = "IDR_SNOWFLAKE_USER", valueFrom = data.aws_ssm_parameter.idr_snowflake_user[0].arn },
      { name = "IDR_SNOWFLAKE_ROLE", valueFrom = data.aws_ssm_parameter.idr_snowflake_role[0].arn }
    ] : []
  )
}

resource "aws_iam_policy" "idr_db_importer_task" {
  name        = "${local.app}-${local.env}-idr-db-importer-task"
  description = "IDR DB Importer ECS task access to S3 bucket and KMS key."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3Access"
        Effect = "Allow"
        Action = [
          "s3:AbortMultipartUpload",
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::${data.aws_ssm_parameter.idr_db_importer_bucket.value}",
          "arn:aws:s3:::${data.aws_ssm_parameter.idr_db_importer_bucket.value}/*"
        ]
      },
      {
        Sid    = "KmsAccessForS3"
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey"
        ]
        Resource = module.platform.kms_alias_primary.target_key_arn
      }
    ]
  })
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
        security_groups  = [data.aws_security_group.idr_db_importer.id]
        subnets          = keys(module.platform.private_subnets)
      }
    }
  }
}

resource "aws_iam_role" "idr_db_importer_eventbridge_scheduler" {
  name = "${local.service_prefix}-idr-db-importer-cron-scheduler-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = ["scheduler.amazonaws.com"]
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_policy" "idr_db_importer_eventbridge_scheduler" {
  name = "${local.service_prefix}-idr-db-importer-cron-scheduler-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "ecs:RunTask"
        ],
        Resource = [
          trimsuffix(module.service.task_definition.arn, ":${module.service.task_definition.revision}"),
          "${trimsuffix(module.service.task_definition.arn, ":${module.service.task_definition.revision}")}:*"
        ],
        Condition = {
          "ArnLike" = {
            "ecs:cluster" = "${data.aws_ecs_cluster.shared.arn}"
          }
        }
      },
      {
        Effect = "Allow",
        Action = [
          "iam:PassRole"
        ]
        Resource = [
          module.service.task_role_arn,
          data.aws_iam_role.idr_db_importer_task_execution.arn
        ]
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "idr_db_importer_eventbridge_scheduler" {
  policy_arn = aws_iam_policy.idr_db_importer_eventbridge_scheduler.arn
  role       = aws_iam_role.idr_db_importer_eventbridge_scheduler.name
}
