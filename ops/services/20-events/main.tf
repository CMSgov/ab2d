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
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/20-events"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "events"

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
  events_image_repo = split("@", data.aws_ecr_image.events.image_uri)[0]
  events_image_tag  = coalesce(var.events_service_image_tag, flatten([[for t in data.aws_ecr_image.events.image_tags : t if strcontains(t, "latest")], data.aws_ecr_image.events.image_tags])[0])
  events_image_uri  = "${local.events_image_repo}:${local.events_image_tag}"

  ab2d_keystore_location_arn = module.platform.ssm.core.keystore_location.arn
  ab2d_keystore_password_arn = module.platform.ssm.core.keystore_password.arn

  ab2d_okta_jwt_issuer_arn      = module.platform.ssm.core.okta_jwt_issuer.arn
  ab2d_slack_alert_webhooks_arn = module.platform.ssm.common.slack_alert_webhooks.arn
  ab2d_slack_trace_webhooks_arn = module.platform.ssm.common.slack_trace_webhooks.arn
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

resource "aws_sns_topic_subscription" "events" {
  topic_arn = data.aws_sns_topic.events.arn
  protocol  = "sqs"
  endpoint  = data.aws_sqs_queue.events.arn
}

module "events_service" {
  source = "github.com/CMSgov/cdap//terraform/modules/service?ref=d9000475e6e2f315ed208f88935ea217ea044fc5"

  cluster_arn           = module.cluster.this.id
  cpu                   = 512
  desired_count         = 1
  execution_role_arn    = data.aws_iam_role.task_execution_role.arn
  force_new_deployment  = anytrue([var.force_events_deployment, var.events_service_image_tag != null])
  image                 = local.events_image_uri
  memory                = 1024
  platform              = module.platform
  security_groups       = [data.aws_security_group.api.id]
  service_name_override = "events"
  task_role_arn         = data.aws_iam_role.task_execution_role.arn

  container_environment = [
    { name = "AB2D_DB_HOST", value = local.ab2d_db_host },
    { name = "AB2D_DB_PORT", value = "5432" },
    { name = "AB2D_DB_SSL_MODE", value = "require" },
    { name = "AB2D_EXECUTION_ENV", value = local.benv },
    { name = "AWS_SQS_FEATURE_FLAG", value = "true" }, #FIXME: is this even used?
    { name = "AWS_SQS_URL", value = local.events_sqs_url },
    { name = "IMAGE_VERSION", value = local.events_image_tag } #FIXME: is this even used?
  ]

  container_secrets = [
    { name = "AB2D_DB_DATABASE", valueFrom = local.db_database_arn },
    { name = "AB2D_DB_PASSWORD", valueFrom = local.db_password_arn },
    { name = "AB2D_DB_USER", valueFrom = local.db_user_arn },
    { name = "AB2D_KEYSTORE_LOCATION", valueFrom = local.ab2d_keystore_location_arn }, #FIXME: is this even used?
    { name = "AB2D_KEYSTORE_PASSWORD", valueFrom = local.ab2d_keystore_password_arn }, #FIXME: is this even used?
    { name = "AB2D_OKTA_JWT_ISSUER", valueFrom = local.ab2d_okta_jwt_issuer_arn },     #FIXME: is this even used?
    { name = "AB2D_SLACK_ALERT_WEBHOOKS", valueFrom = local.ab2d_slack_alert_webhooks_arn },
    { name = "AB2D_SLACK_TRACE_WEBHOOKS", valueFrom = local.ab2d_slack_trace_webhooks_arn }
  ]

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
      hostPort      = 8010 #FIXME is this even necessary?
      containerPort = 8010
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
