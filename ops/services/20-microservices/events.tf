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

module "events_service" {
  source = "github.com/CMSgov/cdap//terraform/modules/service?ref=jscott/PLT-1445"

  cluster_arn                       = module.cluster.this.id
  cpu                               = 512
  desired_count                     = 1
  execution_role_arn                = data.aws_iam_role.task_execution_role.arn
  force_new_deployment              = anytrue([var.force_events_deployment, var.events_service_image_tag != null])
  image                             = local.events_image_uri
  memory                            = 1024
  platform                          = module.platform
  security_groups                   = [data.aws_security_group.api.id]
  service_name_override             = "events"
  task_role_arn                     = data.aws_iam_role.task_execution_role.arn

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
