terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
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
  source    = "github.com/CMSgov/cdap//terraform/modules/platform?ref=ff2ef539fb06f2c98f0e3ce0c8f922bdacb96d66"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = local.app
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-idr-db-importer"
  service     = local.service
}

resource "aws_ecs_task_definition" "idr_db_importer" {
  cpu                      = 1024
  execution_role_arn       = data.aws_iam_role.idr_db_importer_task_execution.arn
  family                   = "${local.service_prefix}-${local.service}"
  memory                   = 2048
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  task_role_arn            = data.aws_iam_role.idr_db_importer_task.arn

  container_definitions = nonsensitive(jsonencode([
    {
      image                  = "${local.image_repo_uri}:${var.image_tag}"
      name                   = local.service
      readonlyRootFilesystem = true

      environment = [
        {
          name  = "AB2D_DB_DATABASE"
          value = data.aws_ssm_parameter.ab2d_db_database.value
        },
        {
          name  = "AB2D_DB_HOST"
          value = data.aws_ssm_parameter.ab2d_db_host.value
        },
        {
          name  = "AB2D_DB_PASSWORD"
          value = data.aws_ssm_parameter.ab2d_db_password.value
        },
        {
          name  = "AB2D_DB_PORT"
          value = "5432"
        },
        {
          name  = "AB2D_DB_USER"
          value = data.aws_ssm_parameter.ab2d_db_user.value
        },
        {
          name  = "S3_BUCKET"
          value = data.aws_ssm_parameter.idr_db_importer_bucket.value
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/aws/ecs/fargate/${local.app}-${local.env}/${local.service}"
          awslogs-create-group  = "true"
          awslogs-region        = var.region
          awslogs-stream-prefix = "${local.app}-${local.env}"
        }
      }
    }
  ]))
}

resource "aws_scheduler_schedule" "idr_db_importer" {
  name       = "${local.service_prefix}-idr-db-importer-eventbridge-schedule"
  group_name = "default"

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression = "cron(0 11 ? * MON-SAT *)" # Every day at 11am UTC except Sunday

  target {
    arn      = data.aws_ecs_cluster.shared.arn
    role_arn = aws_iam_role.idr_db_importer_eventbridge_scheduler.arn

    ecs_parameters {
      task_definition_arn = trimsuffix( # Always use latest
        aws_ecs_task_definition.idr_db_importer.arn,
        ":${aws_ecs_task_definition.idr_db_importer.revision}"
      )
      launch_type = "FARGATE"

      network_configuration {
        assign_public_ip = false
        security_groups  = [data.aws_security_group.idr_db_importer_eventbridge_scheduler.id]
        subnets          = keys(module.platform.private_subnets)
      }
    }
  }
}

resource "aws_iam_role" "idr_db_importer_eventbridge_scheduler" {
  name = "idr-db-importer-cron-scheduler-role"
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
  name = "idr-db-importer-cron-scheduler-policy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "ecs:RunTask"
        ],
        Resource = [
          trimsuffix(aws_ecs_task_definition.idr_db_importer.arn, ":${aws_ecs_task_definition.idr_db_importer.revision}"),
          "${trimsuffix(aws_ecs_task_definition.idr_db_importer.arn, ":${aws_ecs_task_definition.idr_db_importer.revision}")}:*"
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
          data.aws_iam_role.idr_db_importer_task.arn,
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
