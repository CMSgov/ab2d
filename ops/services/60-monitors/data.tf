data "aws_ecs_cluster" "service" {
  for_each = local.ecs_services

  cluster_name = "${local.service_prefix}-${each.key}"
}

data "aws_ecs_service" "this" {
  for_each = local.ecs_services

  cluster_arn  = data.aws_ecs_cluster.service[each.key].arn
  service_name = "${local.service_prefix}-${each.key}"
}

data "aws_ecs_cluster" "shared" {
  cluster_name = local.service_prefix
}

data "aws_sns_topic" "cloudwatch_alarms" {
  name = "${local.service_prefix}-cloudwatch-alarms"
}

data "aws_iam_policy_document" "ecs_events" {
  statement {
    sid    = "AllowEventBridgeToLogEcsEvents"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com", "delivery.logs.amazonaws.com"]
    }

    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.ecs_events.arn}:*"]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        aws_cloudwatch_event_rule.ecs_task_stopped.arn,
        aws_cloudwatch_event_rule.ecs_deployment_failed.arn,
      ]
    }
  }
}
