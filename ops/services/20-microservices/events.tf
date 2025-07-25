locals {
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

resource "aws_sns_topic_subscription" "events" {
  topic_arn = data.aws_sns_topic.events.arn
  protocol  = "sqs"
  endpoint  = data.aws_sqs_queue.events.arn
}

resource "aws_sns_topic_subscription" "splunk_oncall_email_events" {
  topic_arn = data.aws_sns_topic.events.arn
  protocol  = "email"
  endpoint  = data.aws_ssm_parameter.splunk_oncall_email.value
}

resource "aws_ecs_task_definition" "events" {
  family             = "${local.service_prefix}-events"
  network_mode       = "awsvpc"
  execution_role_arn = data.aws_iam_role.task_execution_role.arn
  task_role_arn      = data.aws_iam_role.task_execution_role.arn

  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  container_definitions = nonsensitive(jsonencode([{
    name : "events-service-container", #TODO: Consider simplifying this name, just use "events"
    image : local.events_image_uri,
    essential : true,
    secrets : [
      { name : "AB2D_DB_DATABASE", valueFrom : local.db_database_arn },
      { name : "AB2D_DB_PASSWORD", valueFrom : local.db_password_arn },
      { name : "AB2D_DB_USER", valueFrom : local.db_user_arn },
      { name : "AB2D_KEYSTORE_LOCATION", valueFrom : local.ab2d_keystore_location_arn }, #FIXME: is this even used?
      { name : "AB2D_KEYSTORE_PASSWORD", valueFrom : local.ab2d_keystore_password_arn }, #FIXME: is this even used?
      { name : "AB2D_OKTA_JWT_ISSUER", valueFrom : local.ab2d_okta_jwt_issuer_arn },     #FIXME: is this even used?
      { name : "AB2D_SLACK_ALERT_WEBHOOKS", valueFrom : local.ab2d_slack_alert_webhooks_arn },
      { name : "AB2D_SLACK_TRACE_WEBHOOKS", valueFrom : local.ab2d_slack_trace_webhooks_arn }
    ],
    environment : [
      { name : "AB2D_DB_HOST", value : local.ab2d_db_host },
      { name : "AB2D_DB_PORT", value : "5432" },
      { name : "AB2D_DB_SSL_MODE", value : "require" },
      { name : "AB2D_EXECUTION_ENV", value : local.benv },
      { name : "AWS_SQS_FEATURE_FLAG", value : "true" }, #FIXME: is this even used?
      { name : "AWS_SQS_URL", value : local.events_sqs_url },
      { name : "IMAGE_VERSION", value : local.events_image_tag } #FIXME: is this even used?
    ],
    portMappings : [
      {
        containerPort : 8010 #FIXME is this even necessary?
      }
    ],
    logConfiguration : {
      logDriver : "awslogs",
      options : {
        awslogs-group = "/aws/ecs/fargate/${local.service_prefix}/ab2d_events",
        awslogs-create-group : "true",
        awslogs-region : local.aws_region,
        awslogs-stream-prefix : local.service_prefix
      }
    },
    healthCheck : null
  }]))
}

resource "aws_ecs_service" "events" {
  name                 = "${local.service_prefix}-events"
  cluster              = aws_ecs_cluster.this.id
  task_definition      = aws_ecs_task_definition.events.arn
  desired_count        = 1
  launch_type          = "FARGATE"
  platform_version     = "1.4.0"
  force_new_deployment = anytrue([var.force_events_deployment, var.events_service_image_tag != null])

  network_configuration {
    subnets          = keys(module.platform.private_subnets)
    assign_public_ip = false
    security_groups  = [data.aws_security_group.api.id]
  }
}
