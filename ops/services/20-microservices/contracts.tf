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

resource "aws_ecs_task_definition" "contracts" {
  family                   = "${local.service_prefix}-contracts"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.task_execution_role.arn
  task_role_arn            = data.aws_iam_role.task_execution_role.arn #TODO task/execution role probably ought to be different 😕
  requires_compatibilities = ["FARGATE"]
  cpu                      = 1024
  memory                   = 2048
  container_definitions = nonsensitive(jsonencode([{
    name : "contracts-service-container", #TODO: Consider simplifying this name, just use "contracts"
    image : local.contracts_image_uri,
    essential : true,
    secrets : [
      { name : "AB2D_DB_DATABASE", valueFrom : local.db_database_arn },
      { name : "AB2D_DB_PASSWORD", valueFrom : local.db_password_arn },
      { name : "AB2D_DB_USER", valueFrom : local.db_user_arn },
      { name : "AB2D_HPMS_API_PARAMS", valueFrom : local.hpms_api_params_arn },
      { name : "AB2D_HPMS_URL", valueFrom : local.hpms_url_arn },
      { name : "HPMS_AUTH_KEY_ID", valueFrom : local.hpms_auth_key_id_arn },
      { name : "HPMS_AUTH_KEY_SECRET", valueFrom : local.hpms_auth_key_secret_arn }
    ],
    environment : [
      { name : "AB2D_DB_HOST", value : local.ab2d_db_host },
      { name : "AB2D_DB_PORT", value : "5432" },
      { name : "AB2D_DB_SSL_MODE", value : "require" },
      { name : "AB2D_EXECUTION_ENV", value : local.benv },
      { name : "AWS_SQS_URL", value : local.events_sqs_url },
      { name : "IMAGE_VERSION", value : local.contracts_image_tag }, #FIXME: Is this even used?
      { name : "PROPERTIES_SERVICE_FEATURE_FLAG", value : "true" },  #FIXME: Is this even used?
      { name : "PROPERTIES_SERVICE_URL", value : local.properties_service_url }
    ],
    portMappings : [
      {
        containerPort : 8070
      }
    ],
    logConfiguration : {
      logDriver : "awslogs",
      options : {
        awslogs-group = "/aws/ecs/fargate/${local.service_prefix}/ab2d_contracts",
        awslogs-region : local.aws_region,
        awslogs-stream-prefix : local.service_prefix
      }
    },
    healthCheck : null
  }]))
}

resource "aws_ecs_service" "contracts" {
  name                 = "${local.service_prefix}-contracts"
  cluster              = aws_ecs_cluster.this.id
  task_definition      = aws_ecs_task_definition.contracts.arn
  desired_count        = 1
  launch_type          = "FARGATE"
  platform_version     = "1.4.0"
  force_new_deployment = anytrue([var.force_contracts_deployment, var.contracts_service_image_tag != null])

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
