###############################################################################
# ECS health and failure alarms
#
# These alarms publish to the ab2d-<env>-cloudwatch-alarms SNS topic, which 10-core subscribes to
# the CDAP alarm-to-slack lambda. That lambda parses the app and env out of the alarm name, so alarm
# names must stay prefixed with ab2d-<env>-.
#
###############################################################################

locals {
  ecs_services = toset(["api", "contracts", "events", "worker"])

  # Tasks run on the shared cluster on a schedule rather than as a service, so "below desired
  # count" means nothing for them — we only care that a run failed.
  ecs_scheduled_tasks = toset(["idr-db-importer"])

  ecs_metric_namespace = "AB2D/ECS"
  ecs_alarm_actions    = [data.aws_sns_topic.cloudwatch_alarms.arn]

  ecs_cluster_arns = concat(
    [for cluster in data.aws_ecs_cluster.service : cluster.arn],
    [data.aws_ecs_cluster.shared.arn],
  )

  # Services deploy with deployment_minimum_healthy_percent = 100, so a rolling deployment never
  # takes the running count below desired. Five consecutive minutes below desired means tasks are
  # dying or cannot be placed, not that we are mid-deploy.
  ecs_below_desired_periods = 5
}

resource "aws_cloudwatch_log_group" "ecs_events" {
  name              = "/aws/events/${local.service_prefix}/ecs"
  retention_in_days = 180
  kms_key_id        = module.platform.kms_alias_primary.target_key_arn

  tags = {
    Name = "/aws/events/${local.service_prefix}/ecs"
  }
}

resource "aws_cloudwatch_log_resource_policy" "ecs_events" {
  policy_name     = "${local.service_prefix}-ecs-events"
  policy_document = data.aws_iam_policy_document.ecs_events.json
}

# EventBridge is the only place ECS reports why a task died. Landing the raw events in a log group
# gives us both the alarm source (via metric filters) and the history to debug against.
resource "aws_cloudwatch_event_rule" "ecs_task_stopped" {
  name        = "${local.service_prefix}-ecs-task-stopped"
  description = "Captures stopped ECS tasks across the ${local.env} clusters"

  event_pattern = jsonencode({
    source        = ["aws.ecs"]
    "detail-type" = ["ECS Task State Change"]
    detail = {
      clusterArn = local.ecs_cluster_arns
      lastStatus = ["STOPPED"]
    }
  })
}

resource "aws_cloudwatch_event_target" "ecs_task_stopped" {
  rule      = aws_cloudwatch_event_rule.ecs_task_stopped.name
  target_id = "SendToCloudWatchLogs"
  arn       = aws_cloudwatch_log_group.ecs_events.arn
}

resource "aws_cloudwatch_event_rule" "ecs_deployment_failed" {
  name        = "${local.service_prefix}-ecs-deployment-failed"
  description = "Captures ECS deployments stopped by the deployment circuit breaker in ${local.env}"

  event_pattern = jsonencode({
    source        = ["aws.ecs"]
    "detail-type" = ["ECS Deployment State Change"]
    resources     = [for service in data.aws_ecs_service.this : service.arn]
    detail = {
      eventName = ["SERVICE_DEPLOYMENT_FAILED"]
    }
  })
}

resource "aws_cloudwatch_event_target" "ecs_deployment_failed" {
  rule      = aws_cloudwatch_event_rule.ecs_deployment_failed.name
  target_id = "SendToCloudWatchLogs"
  arn       = aws_cloudwatch_log_group.ecs_events.arn
}

###############################################################################
# A service task stopped on its own
###############################################################################

resource "aws_cloudwatch_log_metric_filter" "task_stopped_unexpectedly" {
  for_each = local.ecs_services

  name           = "${local.service_prefix}-ecs-${each.key}-task-stopped-unexpectedly"
  log_group_name = aws_cloudwatch_log_group.ecs_events.name

  # ServiceSchedulerInitiated covers deployments and scale-in, UserInitiated covers an operator
  # stopping a task, and TerminationNotice covers AWS recycling Fargate infrastructure. Any other
  # stop code means the task died on its own: crash, OOM, failed health check, or a task that could
  # not pull its image or secrets.
  pattern = trimspace(<<-EOT
    { ($.detail.group = "service:${local.service_prefix}-${each.key}") && ($.detail.stopCode != "ServiceSchedulerInitiated") && ($.detail.stopCode != "UserInitiated") && ($.detail.stopCode != "TerminationNotice") }
  EOT
  )

  metric_transformation {
    name      = "${local.service_prefix}-ecs-${each.key}-task-stopped-unexpectedly"
    namespace = local.ecs_metric_namespace
    value     = "1"
    unit      = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "task_stopped_unexpectedly" {
  for_each = local.ecs_services

  alarm_name          = "${local.service_prefix}-ecs-${each.key}-task-stopped-unexpectedly"
  alarm_description   = "An ${each.key} ECS task stopped without a deployment, scale-in, or operator asking it to. Check ${aws_cloudwatch_log_group.ecs_events.name} for the stoppedReason and exit code."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = aws_cloudwatch_log_metric_filter.task_stopped_unexpectedly[each.key].metric_transformation[0].name
  namespace           = local.ecs_metric_namespace
  period              = 300
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"

  alarm_actions = local.ecs_alarm_actions
}

###############################################################################
# A service is running fewer tasks than it should be
###############################################################################

resource "aws_cloudwatch_metric_alarm" "running_tasks_below_desired" {
  for_each = local.ecs_services

  alarm_name          = "${local.service_prefix}-ecs-${each.key}-running-tasks-below-desired"
  alarm_description   = "The ${each.key} ECS service has been running fewer tasks than desired for ${local.ecs_below_desired_periods} minutes, or has stopped reporting task counts entirely."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = 1
  evaluation_periods  = local.ecs_below_desired_periods
  datapoints_to_alarm = local.ecs_below_desired_periods

  # A service whose Container Insights metrics disappear altogether is the quietest failure of all,
  # so absent datapoints count against us rather than being ignored.
  treat_missing_data = "breaching"

  metric_query {
    id          = "shortfall"
    expression  = "IF(desired > running, 1, 0)"
    label       = "${each.key} tasks missing"
    return_data = true
  }

  metric_query {
    id = "desired"
    metric {
      metric_name = "DesiredTaskCount"
      namespace   = "ECS/ContainerInsights"
      period      = 60
      stat        = "Maximum"
      dimensions = {
        ClusterName = data.aws_ecs_cluster.service[each.key].cluster_name
        ServiceName = data.aws_ecs_service.this[each.key].service_name
      }
    }
  }

  metric_query {
    id = "running"
    metric {
      metric_name = "RunningTaskCount"
      namespace   = "ECS/ContainerInsights"
      period      = 60
      stat        = "Minimum"
      dimensions = {
        ClusterName = data.aws_ecs_cluster.service[each.key].cluster_name
        ServiceName = data.aws_ecs_service.this[each.key].service_name
      }
    }
  }

  alarm_actions = local.ecs_alarm_actions
}

###############################################################################
# A deployment never reached steady state
###############################################################################

resource "aws_cloudwatch_log_metric_filter" "deployment_failed" {
  for_each = local.ecs_services

  name           = "${local.service_prefix}-ecs-${each.key}-deployment-failed"
  log_group_name = aws_cloudwatch_log_group.ecs_events.name

  pattern = trimspace(<<-EOT
    { ($.resources[0] = "${data.aws_ecs_service.this[each.key].arn}") && ($.detail.eventName = "SERVICE_DEPLOYMENT_FAILED") }
  EOT
  )

  metric_transformation {
    name      = "${local.service_prefix}-ecs-${each.key}-deployment-failed"
    namespace = local.ecs_metric_namespace
    value     = "1"
    unit      = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "deployment_failed" {
  for_each = local.ecs_services

  alarm_name          = "${local.service_prefix}-ecs-${each.key}-deployment-failed"
  alarm_description   = "The deployment circuit breaker stopped an ${each.key} ECS deployment: the new tasks never became healthy. The service is still serving the previous task definition."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = aws_cloudwatch_log_metric_filter.deployment_failed[each.key].metric_transformation[0].name
  namespace           = local.ecs_metric_namespace
  period              = 300
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"

  alarm_actions = local.ecs_alarm_actions
}

###############################################################################
# A scheduled task failed
###############################################################################

resource "aws_cloudwatch_log_metric_filter" "scheduled_task_failed" {
  for_each = local.ecs_scheduled_tasks

  name           = "${local.service_prefix}-ecs-${each.key}-task-failed"
  log_group_name = aws_cloudwatch_log_group.ecs_events.name

  # Unlike a service, a scheduled task is supposed to exit — so only a non-zero exit code or a task
  # that never got off the ground counts as a failure.
  pattern = trimspace(<<-EOT
    { ($.detail.group = "family:${local.service_prefix}-${each.key}") && (($.detail.stopCode = "TaskFailedToStart") || ($.detail.containers[0].exitCode != 0)) }
  EOT
  )

  metric_transformation {
    name      = "${local.service_prefix}-ecs-${each.key}-task-failed"
    namespace = local.ecs_metric_namespace
    value     = "1"
    unit      = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "scheduled_task_failed" {
  for_each = local.ecs_scheduled_tasks

  alarm_name          = "${local.service_prefix}-ecs-${each.key}-task-failed"
  alarm_description   = "A scheduled ${each.key} ECS task failed to start or exited non-zero. Check ${aws_cloudwatch_log_group.ecs_events.name} for the stoppedReason and exit code."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = aws_cloudwatch_log_metric_filter.scheduled_task_failed[each.key].metric_transformation[0].name
  namespace           = local.ecs_metric_namespace
  period              = 300
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"

  alarm_actions = local.ecs_alarm_actions
}
