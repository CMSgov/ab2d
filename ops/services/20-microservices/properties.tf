locals {
  # Use the provided image tag or get the first, human-readable image tag, favoring a tag with 'latest' in its name if it should exist.
  properties_image_repo = split("@", data.aws_ecr_image.properties.image_uri)[0]
  properties_image_tag  = coalesce(var.properties_service_image_tag, flatten([[for t in data.aws_ecr_image.properties.image_tags : t if strcontains(t, "latest")], data.aws_ecr_image.properties.image_tags])[0])
  properties_image_uri  = "${local.properties_image_repo}:${local.properties_image_tag}"
}

resource "aws_ecs_task_definition" "properties" {
  family                   = "${local.service_prefix}-properties"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.task_execution_role.arn
  task_role_arn            = data.aws_iam_role.task_execution_role.arn
  requires_compatibilities = ["FARGATE"]
  cpu                      = 1024
  memory                   = 2048
  container_definitions = nonsensitive(jsonencode([{
    name : "properties-service-container", #TODO: Consider simplifying this name, just use "properties"
    image : local.properties_image_uri
    essential : true,
    secrets : [
      { name : "AB2D_DB_DATABASE", valueFrom : local.db_database_arn },
      { name : "AB2D_DB_PASSWORD", valueFrom : local.db_password_arn },
      { name : "AB2D_DB_USER", valueFrom : local.db_user_arn }
    ],
    environment : [
      { name : "AB2D_DB_HOST", value : local.ab2d_aurora_endpoint },
      { name : "AB2D_DB_PORT", value : "5432" },
      { name : "AB2D_DB_SSL_MODE", value : "allow" },
      { name : "AB2D_EXECUTION_ENV", value : local.benv },                      #FIXME: Is this even used?
      { name : "IMAGE_VERSION", value : local.properties_image_tag },           #FIXME: Is this even used?
      { name : "PROPERTIES_SERVICE_FEATURE_FLAG", value : "true" },             #FIXME: Is this even used?
      { name : "PROPERTIES_SERVICE_URL", value : local.properties_service_url } # #FIXME: Is this even used?
    ],
    portMappings : [
      {
        containerPort : 8060
      }
    ],
    logConfiguration : {
      logDriver : "awslogs",
      options : {
        awslogs-group : "/aws/ecs/fargate/${local.service_prefix}/ab2d_properties",
        awslogs-create-group : "true",
        awslogs-region : local.aws_region,
        awslogs-stream-prefix : local.service_prefix
      }
    },
    healthCheck : null
  }]))
}

resource "aws_ecs_service" "properties" {
  name                 = "${local.service_prefix}-properties"
  cluster              = aws_ecs_cluster.this.id
  task_definition      = aws_ecs_task_definition.properties.arn
  desired_count        = 1
  launch_type          = "FARGATE"
  platform_version     = "1.4.0"
  force_new_deployment = anytrue([var.force_properties_deployment, var.properties_service_image_tag != null])

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

resource "aws_security_group_rule" "properties_to_worker_egress_access" {
  type                     = "egress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "properties svc to worker sg"
  source_security_group_id = data.aws_security_group.worker.id
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

resource "aws_security_group_rule" "access_to_properties_svc" {
  type                     = "ingress"
  from_port                = 8060
  to_port                  = 8060
  protocol                 = "tcp"
  description              = "for access to properties svc in api sg"
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
