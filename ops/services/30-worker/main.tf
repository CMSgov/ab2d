terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source    = "git::https://github.com/CMSgov/cdap.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = local.app
  env          = local.env
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-worker"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "worker"

  ssm_root_map = {
    common        = "/ab2d/${local.env}/common"
    core          = "/ab2d/${local.env}/core"
    microservices = "/ab2d/${local.env}/microservices"
    worker        = "/ab2d/${local.env}/worker"
  }

  bfd_insights       = "none"
  private_subnet_ids = keys(module.platform.private_subnets)

  #TODO in honor of Ben "Been Jammin'" Hesford
  benv = lookup({
    "dev"     = "ab2d-dev"
    "test"    = "ab2d-east-impl"
    "prod"    = "ab2d-east-prod"
    "sandbox" = "ab2d-sbx-sandbox"
  }, local.parent_env, local.env)

  bfd_url = lookup({
    prod = "https://prod.fhir.bfd.cmscloud.local"
  }, local.parent_env, "https://prod-sbx.fhir.bfd.cmscloud.local")

  ab2d_efs_mount            = "/mnt/efs"
  aws_region                = module.platform.primary_region.name
  bfd_keystore_location     = module.platform.ssm.worker.bfd_keystore_location.value
  bfd_keystore_password_arn = module.platform.ssm.worker.bfd_keystore_password.arn
  vpc_id                    = module.platform.vpc_id

  ecs_task_def_cpu_worker    = module.platform.parent_env == "prod" ? 16384 : 4096
  ecs_task_def_memory_worker = module.platform.parent_env == "prod" ? 32768 : 8192
  max_concurrent_eob_jobs    = "2"
  worker_desired_instances   = 1

  ab2d_db_host              = contains(["dev", "test", "sandbox"], local.parent_env) ? data.aws_rds_cluster.this[0].endpoint : data.aws_db_instance.this[0].address
  db_name_arn               = module.platform.ssm.core.database_name.arn
  db_password_arn           = module.platform.ssm.core.database_password.arn
  db_username_arn           = module.platform.ssm.core.database_user.arn
  microservices_url         = module.platform.ssm.microservices.url.value
  new_relic_app_name        = module.platform.ssm.common.new_relic_app_name.value
  new_relic_license_key_arn = module.platform.ssm.common.new_relic_license_key.arn
  slack_alert_webhooks_arn  = module.platform.ssm.common.slack_alert_webhooks.arn
  slack_trace_webhooks_arn  = module.platform.ssm.common.slack_trace_webhooks.arn

  # Use the provided image tag or get the first, human-readable image tag, favoring a tag with 'latest' in its name if it should exist.
  worker_image_repo = split("@", data.aws_ecr_image.worker.image_uri)[0]
  worker_image_tag  = coalesce(var.worker_service_image_tag, flatten([[for t in data.aws_ecr_image.worker.image_tags : t if strcontains(t, "latest")], data.aws_ecr_image.worker.image_tags])[0])
  worker_image_uri  = "${local.worker_image_repo}:${local.worker_image_tag}"
}

resource "aws_security_group_rule" "egress_worker" {
  type              = "egress"
  description       = "Allow all egress"
  from_port         = "0"
  to_port           = "0"
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = data.aws_security_group.worker.id
}

resource "aws_security_group_rule" "db_access_worker" {
  type                     = "ingress"
  description              = "${local.service_prefix} worker connections"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = data.aws_security_group.db.id
}

resource "aws_security_group_rule" "efs_ingress" {
  type                     = "ingress"
  description              = "NFS"
  from_port                = 2049
  to_port                  = 2049
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = data.aws_security_group.efs.id
}

resource "aws_ecs_cluster" "this" {
  name = "${local.service_prefix}-${local.service}"

  setting {
    name  = "containerInsights"
    value = module.platform.is_ephemeral_env ? "disabled" : "enabled"
  }
}

data "aws_sqs_queue" "events" {
  name = "${local.service_prefix}-events"
}

resource "aws_ecs_task_definition" "worker" {
  family                   = "${local.service_prefix}-${local.service}"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.worker.arn
  task_role_arn            = data.aws_iam_role.worker.arn
  requires_compatibilities = ["FARGATE"]
  cpu                      = local.ecs_task_def_cpu_worker
  memory                   = local.ecs_task_def_memory_worker

  volume {
    name = "efs"

    efs_volume_configuration {
      file_system_id     = data.aws_efs_file_system.this.id
      root_directory     = "/"
      transit_encryption = "ENABLED"
      authorization_config {
        access_point_id = data.aws_efs_access_point.this.id
      }
    }
  }

  container_definitions = nonsensitive(jsonencode([{
    name : local.service,
    image : local.worker_image_uri,
    # Enable read-only root filesystem AB2D-6797
    readonlyRootFilesystem = true
    essential : true,
    mountPoints : [
      {
        containerPath : local.ab2d_efs_mount,
        sourceVolume : "efs"
      }
    ],
    secrets : [
      { name : "AB2D_BFD_KEYSTORE_PASSWORD", valueFrom : local.bfd_keystore_password_arn },
      { name : "AB2D_DB_DATABASE", valueFrom : local.db_name_arn },
      { name : "AB2D_DB_PASSWORD", valueFrom : local.db_password_arn },
      { name : "AB2D_DB_USER", valueFrom : local.db_username_arn },
      { name : "AB2D_SLACK_ALERT_WEBHOOKS", valueFrom : local.slack_alert_webhooks_arn }, #FIXME: Is this even used?
      { name : "AB2D_SLACK_TRACE_WEBHOOKS", valueFrom : local.slack_trace_webhooks_arn }, #FIXME: Is this even used?
      { name : "NEW_RELIC_LICENSE_KEY", valueFrom : local.new_relic_license_key_arn }     #FIXME: Is this even used?
    ]
    environment : [
      { name : "AB2D_BFD_INSIGHTS", value : local.bfd_insights }, #FIXME: Is this even used?
      { name : "AB2D_BFD_KEYSTORE_LOCATION", value : local.bfd_keystore_location },
      { name : "AB2D_BFD_URL", value : local.bfd_url },
      { name : "AB2D_DB_HOST", value : local.ab2d_db_host },
      { name : "AB2D_DB_PORT", value : "5432" },
      { name : "AB2D_DB_SSL_MODE", value : "require" },
      { name : "AB2D_EFS_MOUNT", value : local.ab2d_efs_mount },
      { name : "AB2D_EXECUTION_ENV", value : local.benv },
      { name : "AB2D_JOB_POOL_CORE_SIZE", value : local.max_concurrent_eob_jobs },
      { name : "AB2D_JOB_POOL_MAX_SIZE", value : local.max_concurrent_eob_jobs },
      { name : "AWS_SQS_FEATURE_FLAG", value : "true" }, #FIXME: Is this even used?
      { name : "AWS_SQS_URL", value : data.aws_sqs_queue.events.url },
      { name : "CONTRACTS_SERVICE_FEATURE_FLAG", value : "true" }, #FIXME: Is this even used?
      { name : "IMAGE_VERSION", value : local.worker_image_tag },
      { name : "NEW_RELIC_APP_NAME", value : local.new_relic_app_name },
      { name : "PROPERTIES_SERVICE_FEATURE_FLAG", value : "true" }, #FIXME: Is this even used?
      { name : "PROPERTIES_SERVICE_URL", value : local.microservices_url },
    ],
    logConfiguration : {
      logDriver : "awslogs"
      options : {
        awslogs-group : "/aws/ecs/fargate/${local.service_prefix}/${local.service}",
        awslogs-create-group : "true",
        awslogs-region : local.aws_region,
        awslogs-stream-prefix : local.service_prefix
      }
    },
    healthCheck : null
  }]))
}

resource "aws_ecs_service" "worker" {
  name                               = "${local.service_prefix}-${local.service}"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = coalesce(var.override_task_definition_arn, aws_ecs_task_definition.worker.arn)
  launch_type                        = "FARGATE"
  desired_count                      = local.worker_desired_instances
  force_new_deployment               = anytrue([var.force_worker_deployment, var.worker_service_image_tag != null])
  deployment_minimum_healthy_percent = 100

  network_configuration {
    subnets          = local.private_subnet_ids
    assign_public_ip = false
    security_groups  = [data.aws_security_group.worker.id]
  }
}
