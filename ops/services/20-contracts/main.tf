terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source    = "github.com/CMSgov/cdap//terraform/modules/platform?ref=ff2ef539fb06f2c98f0e3ce0c8f922bdacb96d66"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = local.app
  env          = local.env
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/20-contracts"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "contracts"

  ssm_root_map = {
    common = "/ab2d/${local.env}/common"
    core   = "/ab2d/${local.env}/core"
  }

  benv = lookup({
    "dev"     = "ab2d-dev"
    "test"    = "ab2d-east-impl"
    "prod"    = "ab2d-east-prod"
    "sandbox" = "ab2d-sbx-sandbox"
  }, local.parent_env, local.parent_env)

  ab2d_db_host               = data.aws_rds_cluster.this.endpoint
  aws_account_number         = module.platform.account_id
  aws_region                 = module.platform.primary_region.name
  db_database_arn            = module.platform.ssm.core.database_name.arn
  db_password_arn            = module.platform.ssm.core.database_password.arn
  db_user_arn                = module.platform.ssm.core.database_user.arn
  events_sqs_url             = data.aws_sqs_queue.events.url
  kms_master_key_id          = nonsensitive(module.platform.kms_alias_primary.target_key_arn)
  network_access_logs_bucket = module.platform.network_access_logs_bucket
  vpc_id                     = module.platform.vpc_id

  # Use the provided image tag or get the first, human-readable image tag, favoring a tag with 'latest' in its name if it should exist.
  contracts_image_repo = split("@", data.aws_ecr_image.contracts.image_uri)[0]
  contracts_image_tag  = coalesce(var.contracts_service_image_tag, flatten([[for t in data.aws_ecr_image.contracts.image_tags : t if strcontains(t, "latest")], data.aws_ecr_image.contracts.image_tags])[0])
  contracts_image_uri  = "${local.contracts_image_repo}:${local.contracts_image_tag}"

  hpms_api_params_arn      = module.platform.ssm.core.hpms_api_params.arn
  hpms_auth_key_id_arn     = module.platform.ssm.core.hpms_auth_key_id.arn
  hpms_auth_key_secret_arn = module.platform.ssm.core.hpms_auth_key_secret.arn
  hpms_url_arn             = module.platform.ssm.core.hpms_url.arn
}

module "cluster" {
  source   = "github.com/CMSgov/cdap//terraform/modules/cluster?ref=e06f4acfea302df22c210549effa2e91bc3eff0d"
  platform = module.platform
}

# Chatbot Guardrail Policy
# FIXME No idea where the chatbot/amazonq resources are to be defined
resource "aws_iam_policy" "chatbot_guardrail_policy" {
  name = "${local.service_prefix}-chatbot-guardrail-policy"
  path = "/delegatedadmin/developer/"
  policy = templatefile("${path.module}/templates/config/iam/chatbot_policy.json",
    {
      aws_account_number = local.aws_account_number
      role_name          = data.aws_iam_role.task_execution_role.id
    }
  )
}

# Eventbridge
resource "aws_cloudwatch_event_rule" "this" {
  name          = "${local.service_prefix}-${local.service}-task-monitoring-rule"
  description   = "This rule captures the last status of task definitions"
  event_pattern = <<EOF
{
  "source": ["aws.ecs"],
  "detail-type": ["ECS Task State Change"],
  "detail": {
    "clusterArn": ["${module.cluster.this.arn}"],
    "lastStatus": ["RUNNING"]
  }
}
EOF
}

resource "aws_cloudwatch_event_target" "this" {
  rule      = aws_cloudwatch_event_rule.this.name
  target_id = "SendToSNS"
  arn       = aws_sns_topic.this.arn
}

# SNS
resource "aws_sns_topic" "this" {
  name              = "${local.service_prefix}-${local.service}-monitoring"
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_sns_topic_policy" "this" {
  arn    = aws_sns_topic.this.arn
  policy = data.aws_iam_policy_document.this.json
}

data "aws_iam_policy_document" "this" {
  statement {
    effect  = "Allow"
    actions = ["SNS:Publish"]

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }
    resources = [aws_sns_topic.this.arn]
  }
}

resource "aws_lb" "internal_lb" {
  name               = "${local.service_prefix}-${local.service}"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.internal_lb.id]
  subnets            = keys(module.platform.private_subnets)

  enable_deletion_protection       = !module.platform.is_ephemeral_env
  enable_cross_zone_load_balancing = true
  drop_invalid_header_fields       = true

  access_logs {
    bucket  = local.network_access_logs_bucket
    enabled = true
  }
}

resource "aws_ssm_parameter" "internal_lb" {
  name  = "/ab2d/${local.env}/${local.service}/nonsensitive/url"
  value = "http://${aws_lb.internal_lb.dns_name}"
  type  = "String"
}

resource "aws_lb_listener" "internal_lb" {
  load_balancer_arn = aws_lb.internal_lb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_lb_target_group.contracts.arn
    type             = "forward"
  }
}

resource "aws_security_group" "internal_lb" {
  name   = "${local.service_prefix}-${local.service}-lb"
  vpc_id = module.platform.vpc_id

  tags = { Name = "${local.service_prefix}-${local.service}-lb" }
}

resource "aws_security_group_rule" "lambda_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for lambda to contracts"
  source_security_group_id = data.aws_security_group.lambda.id
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_security_group_rule" "api_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for contracts"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_security_group_rule" "worker_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for contracts"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_security_group_rule" "contracts_to_worker_egress_access" {
  type                     = "egress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "contracts svc to worker sg"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_security_group_rule" "contracts_to_api_egress_access" {
  type                     = "egress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "contracts svc to api sg"
  source_security_group_id = data.aws_security_group.api.id # Api
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_security_group_rule" "access_to_contract_svc" {
  type                     = "ingress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "for access to contract svc in api sg"
  source_security_group_id = aws_security_group.internal_lb.id
  security_group_id        = data.aws_security_group.api.id
}

resource "aws_lb_target_group" "contracts" {
  name        = "${local.service_prefix}-contracts"
  port        = 8070
  protocol    = "HTTP"
  vpc_id      = module.platform.vpc_id
  target_type = "ip"

  health_check {
    path = "/health"
    port = 8070
  }
}

resource "aws_security_group_rule" "contracts_to_lambda_egress_access" {
  type                     = "egress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "contracts svc to lambda"
  source_security_group_id = data.aws_security_group.lambda.id
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_lb_listener_rule" "contracts" {
  listener_arn = aws_lb_listener.internal_lb.arn
  priority     = 200

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.contracts.arn
  }

  condition {
    path_pattern {
      values = ["/contracts", "/contracts/*"]
    }
  }
}

module "contracts_service" {
  source = "github.com/CMSgov/cdap//terraform/modules/service?ref=d9000475e6e2f315ed208f88935ea217ea044fc5"

  cluster_arn                       = module.cluster.this.id
  cpu                               = 1024
  desired_count                     = 1
  execution_role_arn                = data.aws_iam_role.task_execution_role.arn
  force_new_deployment              = anytrue([var.force_contracts_deployment, var.contracts_service_image_tag != null])
  health_check_grace_period_seconds = null
  image                             = local.contracts_image_uri
  memory                            = 2048
  platform                          = module.platform
  security_groups                   = [data.aws_security_group.api.id]
  service_name_override             = "contracts"
  task_role_arn                     = data.aws_iam_role.task_execution_role.arn

  container_environment = [
    { name = "AB2D_DB_HOST", value = local.ab2d_db_host },
    { name = "AB2D_DB_PORT", value = "5432" },
    { name = "AB2D_DB_SSL_MODE", value = "require" },
    { name = "AB2D_EXECUTION_ENV", value = local.benv },
    { name = "AWS_SQS_URL", value = local.events_sqs_url }
  ]

  container_secrets = [
    { name = "AB2D_DB_DATABASE", valueFrom = local.db_database_arn },
    { name = "AB2D_DB_PASSWORD", valueFrom = local.db_password_arn },
    { name = "AB2D_DB_USER", valueFrom = local.db_user_arn },
    { name = "AB2D_HPMS_API_PARAMS", valueFrom = local.hpms_api_params_arn },
    { name = "AB2D_HPMS_URL", valueFrom = local.hpms_url_arn },
    { name = "HPMS_AUTH_KEY_ID", valueFrom = local.hpms_auth_key_id_arn },
    { name = "HPMS_AUTH_KEY_SECRET", valueFrom = local.hpms_auth_key_secret_arn }
  ]

  load_balancers = [{
    target_group_arn = aws_lb_target_group.contracts.arn
    container_name   = "contracts-service-container"
    container_port   = 8070

  }]

  mount_points = [
    {
      "containerPath" = "/tmp",
      "sourceVolume"  = "tmp",
      "readOnly"      = false
    },
    {
      "containerPath" = "/newrelic/logs",
      "sourceVolume"  = "newrelic_logs",
      "readOnly"      = false
    },
    {
      "containerPath" = "/var/log",
      "sourceVolume"  = "var_log",
      "readOnly"      = false
    }
  ]

  port_mappings = [
    {
      containerPort = 8070
      hostPort      = 8070
      protocol      = "tcp"
    }
  ]

  volumes = [
    {
      configure_at_launch = false
      name                = "tmp"
    },
    {
      configure_at_launch = false
      name                = "newrelic_logs"
    },
    {
      configure_at_launch = false
      name                = "var_log"
    }
  ]
}
