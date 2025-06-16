# resource "aws_cloudwatch_metric_alarm" "app_cpu_alarm" {
#   alarm_name          = "${local.service_prefix}-app-cpu-alarm"
#   comparison_operator = "GreaterThanOrEqualToThreshold"
#   evaluation_periods  = "2"
#   metric_name         = "CPUUtilization"
#   namespace           = "AWS/EC2"
#   period              = "300"
#   statistic           = "Average"
#   threshold           = "90.0"
#   alarm_actions       = [local.cloudwatch_sns_topic]

#   dimensions = {
#     AutoScalingGroupName = aws_autoscaling_group.asg.name
#   }
# }

resource "aws_cloudwatch_metric_alarm" "target_response_time" {
  alarm_name          = "${local.service_prefix}-target-response-time"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "TargetResponseTime"
  namespace           = "AWS/ApplicationELB"
  period              = "900"
  statistic           = "Average"
  threshold           = "1.4"
  alarm_actions       = [local.cloudwatch_sns_topic]
  dimensions = {
    LoadBalancer = aws_lb.ab2d_api.arn_suffix
    TargetGroup  = aws_lb_target_group.ab2d_api.arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "healthy_host_count" {
  alarm_name          = "${local.service_prefix}-healthy-host-count"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "300"
  statistic           = "Minimum"
  threshold           = "1.0"
  alarm_actions       = [local.cloudwatch_sns_topic]
  dimensions = {
    LoadBalancer = aws_lb.ab2d_api.arn_suffix
    TargetGroup  = aws_lb_target_group.ab2d_api.arn_suffix
  }
}

# resource "aws_cloudwatch_metric_alarm" "app_status_check_alarm" {
#   alarm_name          = "${local.service_prefix}-app-status-check-alarm"
#   comparison_operator = "GreaterThanOrEqualToThreshold"
#   evaluation_periods  = "1"
#   metric_name         = "StatusCheckFailed"
#   namespace           = "AWS/EC2"
#   period              = "300"
#   statistic           = "Maximum"
#   threshold           = "1.0"
#   alarm_actions       = [local.cloudwatch_sns_topic]
#   dimensions = {
#     AutoScalingGroupName = aws_autoscaling_group.asg.name
#   }
# }

resource "aws_cloudwatch_metric_alarm" "app_elb_http_code_elb_4xx_count" {
  alarm_name          = "${local.service_prefix}-app-elb-http-code-elb_4xx_count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_ELB_4XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Average"
  threshold           = "10.0"
  alarm_actions       = [local.cloudwatch_sns_topic]
  dimensions = {
    LoadBalancer = aws_lb.ab2d_api.arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "http_code_elb_5xx_count" {
  alarm_name          = "${local.service_prefix}-app-elb-http-code_elb-5xx_count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "2"
  metric_name         = "HTTPCode_ELB_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "120"
  statistic           = "Average"
  threshold           = "10.0"
  alarm_actions       = [local.cloudwatch_sns_topic]
  dimensions = {
    LoadBalancer = aws_lb.ab2d_api.arn_suffix
  }
}
