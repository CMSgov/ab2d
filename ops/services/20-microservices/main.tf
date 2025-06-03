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
    common = "/ab2d/${local.parent_env}/common"
    core   = "/ab2d/${local.parent_env}/core"
  }

  benv = lookup({
    "dev"     = "ab2d-dev"
    "test"    = "ab2d-east-impl"
    "prod"    = "ab2d-east-prod"
    "sandbox" = "ab2d-sbx-sandbox"
  }, local.env, local.env)

  ab2d_db_host           = data.aws_db_instance.this.address
  ab2d_db_port           = "5432"
  aws_account_number     = module.platform.account_id
  aws_region             = module.platform.primary_region.name
  events_sqs_url         = data.aws_sqs_queue.events.url
  kms_master_key_id      = nonsensitive(module.platform.kms_alias_primary.target_key_arn)
  properties_service_url = "http://${aws_lb.internal_lb.dns_name}"
  vpc_id                 = module.platform.vpc_id

  contracts_service_image  = data.aws_ecr_image.contracts.image_uri
  events_service_image     = data.aws_ecr_image.events.image_uri
  properties_service_image = data.aws_ecr_image.properties.image_uri

  ab2d_keystore_location_arn    = module.platform.ssm.core.keystore_location.arn
  ab2d_keystore_password_arn    = module.platform.ssm.core.keystore_password.arn
  ab2d_okta_jwt_issuer_arn      = module.platform.ssm.core.okta_jwt_issuer.arn
  ab2d_slack_alert_webhooks_arn = module.platform.ssm.common.slack_alert_webhooks.arn
  ab2d_slack_trace_webhooks_arn = module.platform.ssm.common.slack_trace_webhooks.arn
  db_database_arn               = module.platform.ssm.core.database_name.arn
  db_password_arn               = module.platform.ssm.core.database_password.arn
  db_user_arn                   = module.platform.ssm.core.database_user.arn
  hpms_api_params_arn           = module.platform.ssm.core.hpms_api_params.arn
  hpms_auth_key_id_arn          = module.platform.ssm.core.hpms_auth_key_id.arn
  hpms_auth_key_secret_arn      = module.platform.ssm.core.hpms_auth_key_secret.arn
  hpms_url_arn                  = module.platform.ssm.core.hpms_url.arn
  network_access_logs_bucket    = module.platform.ssm.core.network-access-logs-bucket-name.value
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

# Monitoring
####################################

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

# Load Balancer
####################################
resource "aws_lb" "internal_lb" {
  name               = "${local.service_prefix}-${local.service}"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.internal_lb.id]
  subnets            = keys(module.platform.private_subnets)

  enable_deletion_protection       = true
  enable_cross_zone_load_balancing = true
  drop_invalid_header_fields       = true

  access_logs {
    bucket  = local.network_access_logs_bucket
    prefix  = "${local.service_prefix}-${local.service}"
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

  tags = {
    Name = "${local.service_prefix}-${local.service}-lb"
  }
}
#########################################################################
resource "aws_security_group_rule" "worker_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for microservices"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = aws_security_group.internal_lb.id
}
resource "aws_security_group_rule" "properties_to_worker_egress_access" {
  type                     = "egress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "properties svc to worker sg"
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
#########################################################################
resource "aws_security_group_rule" "api_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for microservices"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = aws_security_group.internal_lb.id
}
resource "aws_security_group_rule" "properties_to_api_egress_access" {
  type                     = "egress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "properties svc to api sg"
  source_security_group_id = data.aws_security_group.api.id # Api
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
#########################################################################
resource "aws_security_group_rule" "access_to_properties_svc" {
  type                     = "ingress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "for access to properties svc in api sg"
  source_security_group_id = aws_security_group.internal_lb.id
  security_group_id        = data.aws_security_group.api.id
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
resource "aws_lb_target_group" "properties" {

  name        = "${local.service_prefix}-properties"
  port        = 8060
  protocol    = "HTTP"
  vpc_id      = module.platform.vpc_id
  target_type = "ip"

  health_check {
    path = "/properties"
    port = 8060
  }
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
resource "aws_lb_listener" "internal_lb" {

  load_balancer_arn = aws_lb.internal_lb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_lb_target_group.properties.id
    type             = "forward"
  }
}

resource "aws_lb_listener_rule" "properties" {
  listener_arn = aws_lb_listener.internal_lb.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.properties.arn
  }

  condition {
    path_pattern {
      values = ["/properties"]
    }
  }
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

# Events Services
####################################
resource "aws_ecs_service" "events" {
  name             = "${local.service_prefix}-events"
  cluster          = aws_ecs_cluster.this.id
  task_definition  = aws_ecs_task_definition.events.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "1.4.0"

  network_configuration {
    subnets          = keys(module.platform.private_subnets)
    assign_public_ip = false
    security_groups  = [data.aws_security_group.api.id]
  }
}
# Event Service SNS
resource "aws_sns_topic_subscription" "events" {
  topic_arn = data.aws_sns_topic.events.arn
  protocol  = "sqs"
  endpoint  = data.aws_sqs_queue.events.arn
}

# Event Service Task Definition
resource "aws_ecs_task_definition" "events" {
  family             = "${local.service_prefix}-events"
  network_mode       = "awsvpc"
  execution_role_arn = data.aws_iam_role.task_execution_role.arn
  task_role_arn      = data.aws_iam_role.task_execution_role.arn

  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  container_definitions = templatefile("${path.module}/templates/config/task_definitions/events_task_def.json",
    {
      events_image                 = local.events_service_image
      events_service_image_version = reverse(split(":", local.events_service_image))[0]
      service_name                 = "ab2d_event"

      ab2d_environment   = local.service_prefix
      ab2d_execution_env = local.benv
      account            = local.aws_account_number
      aws_sqs_url        = local.events_sqs_url
      region             = local.aws_region

      ab2d_db_ssl_mode          = "allow"
      ab2d_db_database          = local.db_database_arn
      ab2d_db_password          = local.db_password_arn
      ab2d_db_host              = local.ab2d_db_host
      ab2d_db_port              = local.ab2d_db_port
      ab2d_db_user              = local.db_user_arn
      ab2d_keystore_location    = local.ab2d_keystore_location_arn
      ab2d_keystore_password    = local.ab2d_keystore_password_arn
      ab2d_okta_jwt_issuer      = local.ab2d_okta_jwt_issuer_arn
      ab2d_slack_alert_webhooks = local.ab2d_slack_alert_webhooks_arn
      ab2d_slack_trace_webhooks = local.ab2d_slack_trace_webhooks_arn
    }
  )
}

# Properties Services
####################################
resource "aws_ecs_service" "properties" {

  name             = "${local.service_prefix}-properties"
  cluster          = aws_ecs_cluster.this.id
  task_definition  = aws_ecs_task_definition.properties.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "1.4.0"

  network_configuration {
    subnets          = keys(module.platform.private_subnets)
    assign_public_ip = false
    security_groups  = [data.aws_security_group.api.id]
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.properties.arn
    container_name   = "properties-service-container"
    container_port   = 8060
  }
}

# Properties Service Task Definition
resource "aws_ecs_task_definition" "properties" {
  family                   = "${local.service_prefix}-properties"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.task_execution_role.arn
  task_role_arn            = data.aws_iam_role.task_execution_role.arn
  requires_compatibilities = ["FARGATE"]
  cpu                      = 1024
  memory                   = 2048
  container_definitions = templatefile("${path.module}/templates/config/task_definitions/properties_task_def.json",
    {
      properties_image                 = local.properties_service_image
      properties_service_url           = local.properties_service_url
      properties_service_image_version = reverse(split(":", local.properties_service_image))[0]
      service_name                     = "ab2d_properties"

      ab2d_db_host     = local.ab2d_db_host
      ab2d_db_port     = local.ab2d_db_port
      ab2d_db_database = local.db_database_arn
      ab2d_db_password = local.db_password_arn
      ab2d_db_user     = local.db_user_arn
      ab2d_db_ssl_mode = "allow"

      account            = local.aws_account_number
      region             = local.aws_region
      ab2d_environment   = local.service_prefix
      ab2d_execution_env = local.benv
    }
  )
}

# Contracts Services
####################################
resource "aws_ecs_service" "contracts" {
  name             = "${local.service_prefix}-contracts"
  cluster          = aws_ecs_cluster.this.id
  task_definition  = aws_ecs_task_definition.contracts.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "1.4.0"

  network_configuration {
    subnets          = keys(module.platform.private_subnets)
    assign_public_ip = false
    security_groups  = [data.aws_security_group.api.id]
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.contracts.arn
    container_name   = "contracts-service-container"
    container_port   = 8070
  }
}

# Contracts Service Task Definition
resource "aws_ecs_task_definition" "contracts" {
  family                   = "${local.service_prefix}-contracts"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.task_execution_role.arn
  task_role_arn            = data.aws_iam_role.task_execution_role.arn #TODO task/execution role probably ought to be different 😕
  requires_compatibilities = ["FARGATE"]
  cpu                      = 1024
  memory                   = 2048
  container_definitions = templatefile("${path.module}/templates/config/task_definitions/contracts_task_def.json",
    {
      contracts_image                 = local.contracts_service_image
      contracts_service_image_version = reverse(split(":", local.contracts_service_image))[0]
      service_name                    = "ab2d_contracts"

      ab2d_efs_mount         = "/mnt/efs"
      ab2d_environment       = local.service_prefix
      ab2d_execution_env     = local.benv
      account                = local.aws_account_number
      aws_sqs_url            = local.events_sqs_url
      region                 = local.aws_region
      properties_service_url = local.properties_service_url

      ab2d_db_ssl_mode     = "allow"
      ab2d_db_database     = local.db_database_arn
      ab2d_db_host         = local.ab2d_db_host
      ab2d_db_port         = local.ab2d_db_port
      ab2d_db_password     = local.db_password_arn
      ab2d_db_user         = local.db_user_arn
      ab2d_hpms_url        = local.hpms_url_arn
      ab2d_hpms_api_params = local.hpms_api_params_arn
      hpms_auth_key_id     = local.hpms_auth_key_id_arn
      hpms_auth_key_secret = local.hpms_auth_key_secret_arn
    }
  )
}

################Lambda Access##################
resource "aws_security_group_rule" "lambda_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for lambda to microservices"
  source_security_group_id = data.aws_security_group.lambda.id
  security_group_id        = aws_security_group.internal_lb.id
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
