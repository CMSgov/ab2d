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
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-api"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "api"

  ssm_root_map = {
    api           = "/ab2d/${local.env}/api"
    common        = "/ab2d/${local.env}/common"
    core          = "/ab2d/${local.env}/core"
    microservices = "/ab2d/${local.env}/microservices"
    contracts     = "/ab2d/mgmt/pdps/nonsensitive/contracts-csv"
    cidrs         = "/ab2d/mgmt/pdps/sensitive/cidr-blocks-csv"
    accounts      = "/ab2d/mgmt/aws-account-numbers"
    mgmt_ipv4     = "/cdap/mgmt/public_nat_ipv4"
  }

  #TODO in honor of Ben "Been Jammin'" Hesford
  benv = lookup({
    dev     = "ab2d-dev"
    test    = "ab2d-east-impl"
    prod    = "ab2d-east-prod"
    sandbox = "ab2d-sbx-sandbox"
  }, local.parent_env, local.parent_env)

  ab2d_efs_mount               = "/mnt/efs"
  aws_region                   = module.platform.primary_region.name
  ab2d_keystore_location       = module.platform.ssm.core.keystore_location.value
  ab2d_keystore_password_arn   = module.platform.ssm.core.keystore_password.arn
  ab2d_okta_jwt_issuer_arn     = module.platform.ssm.core.okta_jwt_issuer.arn
  alb_internal                 = false
  alb_listener_certificate_arn = module.platform.is_ephemeral_env || local.tls_private_key == null ? null : aws_acm_certificate.this[0].arn
  alb_listener_port            = module.platform.is_ephemeral_env ? 80 : 443
  alb_listener_protocol        = module.platform.is_ephemeral_env ? "HTTP" : "HTTPS"
  api_desired_instances        = module.platform.parent_env == "prod" ? 2 : 1
  bfd_insights                 = "none" #FIXME?
  container_port               = 8443
  db_name_arn                  = module.platform.ssm.core.database_name.arn
  db_password_arn              = module.platform.ssm.core.database_password.arn
  db_username_arn              = module.platform.ssm.core.database_user.arn
  ecs_task_def_cpu_api         = 4096  #FIXME: this needs to be environment-specific
  ecs_task_def_memory_api      = 14336 #FIXME: this needs to be environment-specific
  hpms_api_params_arn          = module.platform.ssm.core.hpms_api_params.arn
  hpms_auth_key_id_arn         = module.platform.ssm.core.hpms_auth_key_id.arn
  hpms_auth_key_secret_arn     = module.platform.ssm.core.hpms_auth_key_secret.arn
  hpms_url_arn                 = module.platform.ssm.core.hpms_url.arn
  kms_master_key_id            = nonsensitive(module.platform.kms_alias_primary.target_key_arn)
  microservices_url            = lookup(module.platform.ssm.microservices, "url", { value : "none" }).value
  network_access_logs_bucket   = module.platform.ssm.core.network-access-logs-bucket-name.value
  new_relic_app_name           = module.platform.ssm.common.new_relic_app_name.value
  new_relic_license_key_arn    = module.platform.ssm.common.new_relic_license_key.arn
  private_subnet_ids           = keys(module.platform.private_subnets)
  public_subnet_ids            = keys(module.platform.public_subnets)
  slack_alert_webhooks_arn     = module.platform.ssm.common.slack_alert_webhooks.arn
  slack_trace_webhooks_arn     = module.platform.ssm.common.slack_trace_webhooks.arn
  vpc_id                       = module.platform.vpc_id
  cloudwatch_sns_topic         = data.aws_sns_topic.cloudwatch_alarms.arn

  pdp_map = { for k in keys(module.platform.ssm.cidrs) : k => { "cidrs" = nonsensitive(module.platform.ssm.cidrs[k].value), "contracts" = nonsensitive(module.platform.ssm.contracts[k].value) } }

  # Use the provided image tag or get the first, human-readable image tag, favoring a tag with 'latest' in its name if it should exist.
  api_image_repo = split("@", data.aws_ecr_image.api.image_uri)[0]
  api_image_tag  = coalesce(var.api_service_image_tag, flatten([[for t in data.aws_ecr_image.api.image_tags : t if strcontains(t, "latest")], data.aws_ecr_image.api.image_tags])[0])
  api_image_uri  = "${local.api_image_repo}:${local.api_image_tag}"

  tls_private_key = lookup(module.platform.ssm.api, "tls_private_key", { value : null }).value
  tls_public_cert = lookup(module.platform.ssm.api, "tls_public_cert", { value : null }).value
  tls_chain       = lookup(module.platform.ssm.api, "tls_chain", { value : null }).value
}

resource "aws_security_group" "pdp" {
  name        = "${local.service_prefix}-pdp"
  description = "PDP security group"
  vpc_id      = local.vpc_id
  tags = {
    Name = "${local.service_prefix}-pdp"
  }
}

resource "aws_security_group_rule" "host_port" {
  type                     = "ingress"
  description              = "Host Port"
  from_port                = local.alb_listener_port
  to_port                  = local.alb_listener_port
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = data.aws_security_group.api.id
}

resource "aws_security_group" "load_balancer" {
  name        = "${local.service_prefix}-load-balancer-sg"
  description = "API load balancer security group"
  vpc_id      = local.vpc_id
  tags = {
    Name = "${local.service_prefix}-load-balancer-sg"
  }
}

resource "aws_security_group_rule" "load_balancer_api" {
  type                     = "ingress"
  description              = "Allow loadbalancer access to api workload on ${local.alb_listener_port}"
  from_port                = local.container_port
  to_port                  = local.container_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.load_balancer.id
  security_group_id        = data.aws_security_group.api.id
}

resource "aws_security_group_rule" "egress_lb" {
  type              = "egress"
  description       = "Allow all egress"
  from_port         = "0"
  to_port           = "0"
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "load_balancer_access_mgmt" {
  for_each = nonsensitive(module.platform.ssm.mgmt_ipv4)

  type              = "ingress"
  description       = "Access from ${each.key}"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = ["${each.value.value}/32"]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "load_balancer_access_nat" {
  for_each = nonsensitive(module.platform.nat_gateways)

  type              = "ingress"
  description       = "${local.env} ${lookup(each.value.tags, "Name", "nat")} website access"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = ["${each.value.public_ip}/32"]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "pdp" {
  for_each = nonsensitive(local.pdp_map)

  type              = "ingress"
  description       = "${replace(each.key, "-", " ")}: ${each.value["contracts"]}"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = split(", ", each.value["cidrs"])
  security_group_id = aws_security_group.pdp.id
}

resource "aws_security_group_rule" "open_access_sandbox" {
  count             = local.env == "sandbox" ? 1 : 0
  type              = "ingress"
  description       = "Rule to open SBX to public"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.pdp.id
}

resource "aws_security_group_rule" "cdap_ingress" {
  type              = "ingress"
  description       = "CDAP Platform Ingress"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = [module.platform.platform_cidr]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "efs_ingress" {
  type                     = "ingress"
  description              = "NFS"
  from_port                = 2049
  to_port                  = 2049
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = data.aws_security_group.efs.id
}

resource "aws_ecs_cluster" "ab2d_api" {
  name = "${local.service_prefix}-${local.service}"

  setting {
    name  = "containerInsights"
    value = module.platform.is_ephemeral_env ? "disabled" : "enabled"
  }
}

resource "aws_ecs_task_definition" "api" {
  family                   = "${local.service_prefix}-${local.service}"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.api.arn
  task_role_arn            = data.aws_iam_role.api.arn
  requires_compatibilities = ["FARGATE"]
  track_latest             = false
  cpu                      = local.ecs_task_def_cpu_api
  memory                   = local.ecs_task_def_memory_api

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
    image : local.api_image_uri,
    essential : true,
    portMappings : [
      {
        containerPort : local.container_port,
      }
    ],
    mountPoints : [
      {
        containerPath : local.ab2d_efs_mount,
        sourceVolume : "efs"
      }
    ],
    secrets : [
      { name : "AB2D_DB_DATABASE", valueFrom : local.db_name_arn },
      { name : "AB2D_DB_PASSWORD", valueFrom : local.db_password_arn },
      { name : "AB2D_DB_USER", valueFrom : local.db_username_arn },
      { name : "AB2D_HPMS_API_PARAMS", valueFrom : local.hpms_api_params_arn }, #FIXME: Is this even used?
      { name : "AB2D_HPMS_URL", valueFrom : local.hpms_url_arn },               #FIXME: Is this even used?
      { name : "AB2D_KEYSTORE_PASSWORD", valueFrom : local.ab2d_keystore_password_arn },
      { name : "AB2D_OKTA_JWT_ISSUER", valueFrom : local.ab2d_okta_jwt_issuer_arn },
      { name : "AB2D_SLACK_ALERT_WEBHOOKS", valueFrom : local.slack_alert_webhooks_arn }, #FIXME: Is this even used?
      { name : "AB2D_SLACK_TRACE_WEBHOOKS", valueFrom : local.slack_trace_webhooks_arn }, #FIXME: Is this even used?
      { name : "HPMS_AUTH_KEY_ID", valueFrom : local.hpms_auth_key_id_arn },              #FIXME: Is this even used?
      { name : "HPMS_AUTH_KEY_SECRET", valueFrom : local.hpms_auth_key_secret_arn },      #FIXME: Is this even used?
      { name : "NEW_RELIC_LICENSE_KEY", valueFrom : local.new_relic_license_key_arn },    #FIXME: Is this even used?
    ],
    environment : [
      { name : "AB2D_BFD_INSIGHTS", value : local.bfd_insights }, #FIXME: Is this even used?
      { name : "AB2D_DB_HOST", value : data.aws_db_instance.this.address },
      { name : "AB2D_DB_PORT", value : "5432" },
      { name : "AB2D_DB_SSL_MODE", value : "require" },
      { name : "AB2D_EFS_MOUNT", value : local.ab2d_efs_mount },
      { name : "AB2D_EXECUTION_ENV", value : local.benv }, #FIXME: Is this even used?
      { name : "AB2D_KEYSTORE_LOCATION", value : local.ab2d_keystore_location },
      { name : "AB2D_V2_ENABLED", value : "true" },
      { name : "AWS_SQS_FEATURE_FLAG", value : "true" },
      { name : "AWS_SQS_URL", value : data.aws_sqs_queue.events.url }, #FIXME: Is this even used?
      { name : "CONTRACTS_SERVICE_FEATURE_FLAG", value : "true" },     #FIXME: Is this even used?
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

resource "aws_ecs_service" "api" {
  name                               = "${local.service_prefix}-${local.service}"
  cluster                            = aws_ecs_cluster.ab2d_api.id
  task_definition                    = coalesce(var.override_task_definition_arn, aws_ecs_task_definition.api.arn)
  launch_type                        = "FARGATE"
  desired_count                      = local.api_desired_instances
  force_new_deployment               = anytrue([var.force_api_deployment, var.api_service_image_tag != null])
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds  = 180

  network_configuration {
    subnets          = local.private_subnet_ids
    assign_public_ip = false
    security_groups  = [data.aws_security_group.api.id, aws_security_group.load_balancer.id] #FIXME
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.ab2d_api.arn
    container_name   = local.service
    container_port   = local.container_port
  }
}

resource "aws_sns_topic" "api" {
  name              = "${local.service_prefix}-api-healthy-host"
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_cloudwatch_metric_alarm" "health" {
  alarm_name          = "${local.service_prefix}-api-healthy-host"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "20"
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Maximum"
  threshold           = "1"
  alarm_description   = "Healthy host count for API target group"
  alarm_actions       = [aws_sns_topic.api.arn]
  ok_actions          = [aws_sns_topic.api.arn]

  dimensions = {
    LoadBalancer = aws_lb.ab2d_api.arn_suffix
    TargetGroup  = aws_lb_target_group.ab2d_api.arn_suffix
  }
}

data "aws_caller_identity" "current" {}

resource "aws_lb" "ab2d_api" {
  #TODO Consider using name_prefix for ephemeral environments... thhey may only be up to 6-characters
  name               = "${local.service_prefix}-api"
  depends_on         = [aws_lb_target_group.ab2d_api]
  internal           = local.alb_internal
  load_balancer_type = "application"

  security_groups = [
    data.aws_security_group.api.id,
    aws_security_group.load_balancer.id,
    aws_security_group.pdp.id,
    module.platform.security_groups["zscaler-public"].id,
  ]

  subnets = local.public_subnet_ids

  enable_deletion_protection       = contains(["prod", "sandbox", "test", "dev"], local.env)
  enable_cross_zone_load_balancing = true
  drop_invalid_header_fields       = true

  access_logs {
    bucket  = "cms-cloud-${data.aws_caller_identity.current.account_id}-us-east-1"
    enabled = true
  }
}

resource "aws_lb_target_group" "ab2d_api" {
  name        = "${local.service_prefix}-api"
  port        = local.alb_listener_port
  protocol    = local.alb_listener_protocol
  vpc_id      = local.vpc_id
  target_type = "ip"

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 5
    timeout             = 10
    protocol            = local.alb_listener_protocol
    path                = "/health"
    interval            = 30
  }
}

resource "aws_lb_listener" "ab2d_api" {
  load_balancer_arn = aws_lb.ab2d_api.arn
  port              = local.alb_listener_port
  protocol          = local.alb_listener_protocol
  certificate_arn   = local.alb_listener_certificate_arn

  default_action {
    target_group_arn = aws_lb_target_group.ab2d_api.arn
    type             = "forward"
  }
}

resource "aws_acm_certificate" "this" {
  count             = local.tls_private_key != null ? 1 : 0
  private_key       = local.tls_private_key
  certificate_body  = local.tls_public_cert
  certificate_chain = local.tls_chain
}
