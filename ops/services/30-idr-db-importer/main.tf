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
  family                   = "${local.service_prefix}-${local.service}"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.idr_db_importer_execution.arn
  task_role_arn            = data.aws_iam_role.idr_db_importer.arn
  requires_compatibilities = ["FARGATE"]
  cpu                      = 1024
  memory                   = 2048

  container_definitions = nonsensitive(jsonencode([
    {
      name                   = local.service
      image                  = "${local.image_repo_uri}:${var.image_tag}"
      readonlyRootFilesystem = true
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
