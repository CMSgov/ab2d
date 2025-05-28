module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=267771f3414c92e2f3090616587550e26bc41a47"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = "ab2d"
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d-ops/tree/main/terraform/services/microservices"
  service     = local.service
}

locals {
  service      = "services"
  default_tags = module.platform.default_tags
  benv = lookup({
    "dev"     = "ab2d-dev"
    "test"    = "ab2d-east-impl"
    "prod"    = "ab2d-east-prod"
    "sandbox" = "ab2d-sbx-sandbox"
  }, local.env, local.env)

  aws_account_number     = module.platform.account_id
  env                    = terraform.workspace
  aws_region             = module.platform.primary_region.name
  service_prefix         = "ab2d-${local.env}"
  aws_sqs_url            = data.aws_sqs_queue.ab2d_sqs.url
  properties_service_url = "http://${aws_lb.internal_lb.dns_name}"
  ab2d_db_host           = data.aws_db_instance.this.address
  ab2d_db_port           = "5432"
  vpc_id                 = module.platform.vpc_id

  contracts_service_image  = data.aws_ecr_image.contracts_service_image.image_uri
  events_service_image     = data.aws_ecr_image.events_service_image.image_uri
  properties_service_image = data.aws_ecr_image.properties_service_image.image_uri

  db_database_arn               = data.aws_ssm_parameter.database_name.arn #TODO this is derivable
  db_password_arn               = data.aws_ssm_parameter.database_password.arn
  db_user_arn                   = data.aws_ssm_parameter.database_user.arn
  ab2d_keystore_location_arn    = data.aws_ssm_parameter.keystore_location.arn #TODO this appears to be an invariant
  ab2d_keystore_password_arn    = data.aws_ssm_parameter.keystore_password.arn
  ab2d_okta_jwt_issuer_arn      = data.aws_ssm_parameter.okta_jwt_issuer.arn
  ab2d_slack_alert_webhooks_arn = data.aws_ssm_parameter.slack_alert_webhooks.arn
  ab2d_slack_trace_webhooks_arn = data.aws_ssm_parameter.slack_trace_webhooks.arn

  hpms_url_arn             = data.aws_ssm_parameter.hpms_url.arn
  hpms_api_params_arn      = data.aws_ssm_parameter.hpms_api_params.arn
  hpms_auth_key_id_arn     = data.aws_ssm_parameter.hpms_auth_key_id.arn
  hpms_auth_key_secret_arn = data.aws_ssm_parameter.hpms_auth_key_secret.arn
}

data "aws_ecr_image" "contracts_service_image" {
  repository_name = "ab2d-services"
  image_tag       = var.contracts_service_image_tag
}

data "aws_ecr_image" "events_service_image" {
  repository_name = "ab2d-services"
  image_tag       = var.events_service_image_tag
}

data "aws_ecr_image" "properties_service_image" {
  repository_name = "ab2d-services"
  image_tag       = var.properties_service_image_tag
}

# ECS Cluster
####################################
resource "aws_ecs_cluster" "ab2d_ecs_cluster" {
  name = "${local.service_prefix}-microservice-cluster"
}

# Monitoring
####################################

# Chatbot Guardrail Policy  
resource "aws_iam_policy" "chatbot_guardrail_policy" {
  name = "${local.service_prefix}-chatbot-guardrail-policy"
  path = "/delegatedadmin/developer/"
  policy = templatefile("${path.module}/templates/config/iam/chatbot_policy.json",
    {
      aws_account_number = local.aws_account_number
    }
  )
}

# Eventbridge
resource "aws_cloudwatch_event_rule" "ab2d-microservice-eventbridge" {
  name          = "${local.service_prefix}-microservice-task-monitoring-rule"
  description   = "This rule captures the last status of task definitions"
  event_pattern = <<EOF
{
  "source": ["aws.ecs"],
  "detail-type": ["ECS Task State Change"],
  "detail": {
    "clusterArn": ["${aws_ecs_cluster.ab2d_ecs_cluster.arn}"],
    "lastStatus": ["RUNNING"]
  }
}
EOF
}

resource "aws_cloudwatch_event_target" "ab2d-sns" {
  rule      = aws_cloudwatch_event_rule.ab2d-microservice-eventbridge.name
  target_id = "SendToSNS"
  arn       = aws_sns_topic.ab2d-microservice-sns.arn
}

# SNS
resource "aws_sns_topic" "ab2d-microservice-sns" {
  name = "${local.service_prefix}-microservice-monitoring-sns"
}

resource "aws_sns_topic_policy" "ab2d-microservice-sns-policy" {
  arn    = aws_sns_topic.ab2d-microservice-sns.arn
  policy = data.aws_iam_policy_document.ab2d-microservice-sns-topic-policy.json
}

data "aws_iam_policy_document" "ab2d-microservice-sns-topic-policy" {
  statement {
    effect  = "Allow"
    actions = ["SNS:Publish"]

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }
    resources = [aws_sns_topic.ab2d-microservice-sns.arn]
  }
}

# Load Balancer
####################################
data "aws_ssm_parameter" "lb_access_logs_bucket" {
  #NOTE defined as part of ab2d `core`
  name = "/ab2d/${local.env}/lb-access-logs-bucket-name"
}

resource "aws_lb" "internal_lb" {
  name               = "${local.service_prefix}-microservice-lb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.internal_lb_sg.id]
  subnets            = keys(module.platform.private_subnets)

  enable_deletion_protection       = true
  enable_cross_zone_load_balancing = true
  drop_invalid_header_fields       = true

  access_logs {
    bucket  = data.aws_ssm_parameter.lb_access_logs_bucket.insecure_value
    prefix  = "${local.service_prefix}-microservice-lb"
    enabled = true
  }
}

#FIXME: Eliminate usage of secrets manager for divining properties_service_url in worker and api modules
resource "aws_ssm_parameter" "internal_lb" {
  name  = "/ab2d/${local.env}/services/internal_lb"
  value = "http://${aws_lb.internal_lb.dns_name}"
  type  = "String"
}

resource "aws_security_group" "internal_lb_sg" {
  name   = "${local.service_prefix}-microservice-lb-sg"
  vpc_id = module.platform.vpc_id

  tags = {
    Name = "${local.service_prefix}-microservice-lb-sg"
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
  security_group_id        = aws_security_group.internal_lb_sg.id
}
resource "aws_security_group_rule" "properties_to_worker_egress_access" {
  type                     = "egress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "properties svc to worker sg"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = aws_security_group.internal_lb_sg.id
}
resource "aws_security_group_rule" "contracts_to_worker_egress_access" {
  type                     = "egress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "contracts svc to worker sg"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = aws_security_group.internal_lb_sg.id
}
#########################################################################
resource "aws_security_group_rule" "api_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  description              = "inbound access for microservices"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = aws_security_group.internal_lb_sg.id
}
resource "aws_security_group_rule" "properties_to_api_egress_access" {
  type                     = "egress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "properties svc to api sg"
  source_security_group_id = data.aws_security_group.api.id # Api
  security_group_id        = aws_security_group.internal_lb_sg.id
}
resource "aws_security_group_rule" "contracts_to_api_egress_access" {
  type                     = "egress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "contracts svc to api sg"
  source_security_group_id = data.aws_security_group.api.id # Api
  security_group_id        = aws_security_group.internal_lb_sg.id
}
#########################################################################
resource "aws_security_group_rule" "access_to_properties_svc" {
  type                     = "ingress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "for access to properties svc in api sg"
  source_security_group_id = aws_security_group.internal_lb_sg.id
  security_group_id        = data.aws_security_group.api.id
}
resource "aws_security_group_rule" "access_to_contract_svc" {
  type                     = "ingress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "for access to contract svc in api sg"
  source_security_group_id = aws_security_group.internal_lb_sg.id
  security_group_id        = data.aws_security_group.api.id
}
resource "aws_lb_target_group" "properties_tg" {

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

resource "aws_lb_target_group" "contracts_tg" {
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
resource "aws_lb_listener" "internal_lb_listener" {

  load_balancer_arn = aws_lb.internal_lb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_lb_target_group.properties_tg.id
    type             = "forward"
  }

}

resource "aws_lb_listener_rule" "properties_svc_rule" {
  listener_arn = aws_lb_listener.internal_lb_listener.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.properties_tg.arn
  }

  condition {
    path_pattern {
      values = ["/properties"]
    }
  }

}

resource "aws_lb_listener_rule" "contracts_svc_rule" {
  listener_arn = aws_lb_listener.internal_lb_listener.arn
  priority     = 200

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.contracts_tg.arn
  }

  condition {
    path_pattern {
      values = ["/contracts", "/contracts/*"]
    }
  }

}

# Events Services
####################################
resource "aws_ecs_service" "events_service" {
  name             = "${local.service_prefix}-event-service"
  cluster          = aws_ecs_cluster.ab2d_ecs_cluster.id
  task_definition  = aws_ecs_task_definition.ab2d-event-service-task-definition.arn
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
data "aws_sns_topic" "ab2d-sns-topic" {
  name = "${local.service_prefix}-events-sns-topic"
}

data "aws_sqs_queue" "ab2d_sqs" {
  name = "${local.service_prefix}-events-sqs"
}

resource "aws_sns_topic_subscription" "ab2d-sns-topic-subscription" {
  topic_arn = data.aws_sns_topic.ab2d-sns-topic.arn
  protocol  = "sqs"
  endpoint  = data.aws_sqs_queue.ab2d_sqs.arn
}

# Event Service Task Definition
resource "aws_ecs_task_definition" "ab2d-event-service-task-definition" {
  family             = "${local.service_prefix}-event-service-task-definition"
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
      aws_sqs_url        = local.aws_sqs_url
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
resource "aws_ecs_service" "properties_service" {

  name             = "${local.service_prefix}-properties-service"
  cluster          = aws_ecs_cluster.ab2d_ecs_cluster.id
  task_definition  = aws_ecs_task_definition.ab2d-properties-service-task-definition.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "1.4.0"

  network_configuration {
    subnets          = keys(module.platform.private_subnets)
    assign_public_ip = false
    security_groups  = [data.aws_security_group.api.id]
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.properties_tg.arn
    container_name   = "properties-service-container"
    container_port   = 8060
  }
}

# Properties Service Task Definition
resource "aws_ecs_task_definition" "ab2d-properties-service-task-definition" {
  family                   = "${local.service_prefix}-properties-service-task-definition"
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
resource "aws_ecs_service" "contract_service" {

  name             = "ab2d-contracts-service"
  cluster          = aws_ecs_cluster.ab2d_ecs_cluster.id
  task_definition  = aws_ecs_task_definition.ab2d-contracts-service-task-definition.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "1.4.0"

  network_configuration {
    subnets          = keys(module.platform.private_subnets)
    assign_public_ip = false
    security_groups  = [data.aws_security_group.api.id]
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.contracts_tg.arn
    container_name   = "contracts-service-container"
    container_port   = 8070
  }
}

# Contracts Service Task Definition
resource "aws_ecs_task_definition" "ab2d-contracts-service-task-definition" {
  family                   = "${local.service_prefix}-contracts-service-task-definition"
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
      aws_sqs_url            = local.aws_sqs_url
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
  description              = "inbound access for lambda to microservice"
  source_security_group_id = data.aws_security_group.lambda.id
  security_group_id        = aws_security_group.internal_lb_sg.id
}

resource "aws_security_group_rule" "contracts_to_lambda_egress_access" {
  type                     = "egress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "contracts svc to lambda sg"
  source_security_group_id = data.aws_security_group.lambda.id
  security_group_id        = aws_security_group.internal_lb_sg.id
}
