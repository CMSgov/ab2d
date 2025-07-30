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
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/20-microservices"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "microservices"

  ssm_root_map = {
    common = "/ab2d/${local.env}/common"
    core   = "/ab2d/${local.env}/core"
    splunk = "/ab2d/mgmt/splunk"
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
  properties_service_url     = "http://${aws_lb.internal_lb.dns_name}"
  vpc_id                     = module.platform.vpc_id
}

# ECS Cluster
####################################
resource "aws_ecs_cluster" "this" {
  name = "${local.service_prefix}-${local.service}"

  setting {
    name  = "containerInsights"
    value = module.platform.is_ephemeral_env ? "disabled" : "enabled"
  }

  # FIXME Enable the below, along with enabling the cluster acess to the CMK
  # configuration {
  #   managed_storage_configuration {
  #     fargate_ephemeral_storage_kms_key_id = local.kms_master_key_id
  #     kms_key_id                           = local.kms_master_key_id
  #   }
  # }
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
    "clusterArn": ["${aws_ecs_cluster.this.arn}"],
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
    target_group_arn = aws_lb_target_group.properties.id
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
  description              = "inbound access for lambda to microservices"
  source_security_group_id = data.aws_security_group.lambda.id
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_security_group_rule" "api_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for microservices"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = aws_security_group.internal_lb.id
}

resource "aws_security_group_rule" "worker_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for microservices"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = aws_security_group.internal_lb.id
}
