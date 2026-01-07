locals {
  # Use the provided image tag or get the first, human-readable image tag, favoring a tag with 'latest' in its name if it should exist.
  contracts_image_repo = split("@", data.aws_ecr_image.contracts.image_uri)[0]
  contracts_image_tag  = coalesce(var.contracts_service_image_tag, flatten([[for t in data.aws_ecr_image.contracts.image_tags : t if strcontains(t, "latest")], data.aws_ecr_image.contracts.image_tags])[0])
  contracts_image_uri  = "${local.contracts_image_repo}:${local.contracts_image_tag}"

  hpms_api_params_arn      = module.platform.ssm.core.hpms_api_params.arn
  hpms_auth_key_id_arn     = module.platform.ssm.core.hpms_auth_key_id.arn
  hpms_auth_key_secret_arn = module.platform.ssm.core.hpms_auth_key_secret.arn
  hpms_url_arn             = module.platform.ssm.core.hpms_url.arn
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
  source = "github.com/CMSgov/cdap//terraform/modules/service?ref=jscott/PLT-1445"

  # awslogs_group_override            = "ab2d_contracts"
  cluster_arn                       = module.cluster.this.id
  # container_name_override           = "contracts-service-container"
  cpu                               = 1024
  desired_count                     = 1
  execution_role_arn                = data.aws_iam_role.task_execution_role.arn
  force_new_deployment              = anytrue([var.force_contracts_deployment, var.contracts_service_image_tag != null])
  health_check_grace_period_seconds = null
  image                             = local.contracts_image_uri
  memory                            = 2048
  platform                          = module.platform
  platform_version                  = "1.4.0"
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
