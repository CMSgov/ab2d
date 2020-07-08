resource "aws_cloudwatch_metric_alarm" "app_cpu_alarm" {
  alarm_name = "${var.env}-app-cpu-alarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "2"
  metric_name = "CPUUtilization"
  namespace = "AWS/EC2"
  period = "300"
  statistic = "Average"
  threshold = "90.0"
  # alarm_actions = [var.autoscaling_arn, var.sns_arn]
  dimensions = {
    AutoScalingGroupName = var.autoscaling_name
  }
}

resource "aws_cloudwatch_metric_alarm" "target_response_time" {
  alarm_name = "${var.env}-target-response-time"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "TargetResponseTime"
  namespace = "AWS/ApplicationELB"
  period = "900"
  statistic = "Average"
  threshold = "1.4"
  # alarm_actions = [var.autoscaling_arn, var.sns_arn]
  dimensions = {
    TargetGroup = var.target_group_arn_suffix
    LoadBalancer = var.loadbalancer_arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "healthy_host_count" {
  alarm_name = "${var.env}-healthy-host-count"
  comparison_operator = "LessThanThreshold"
  evaluation_periods = "1"
  metric_name = "HealthyHostCount"
  namespace = "AWS/ApplicationELB"
  period = "60"
  statistic = "Minimum"
  threshold = "1.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    TargetGroup = var.target_group_arn_suffix
    LoadBalancer = var.loadbalancer_arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "app_status_check_alarm" {
  alarm_name = "${var.env}-app-status-check-alarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "StatusCheckFailed"
  namespace = "AWS/EC2"
  period = "60"
  statistic = "Maximum"
  threshold = "1.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    AutoScalingGroupName = var.autoscaling_name
  }
}

resource "aws_cloudwatch_metric_alarm" "controller_cpu_alarm" {
  alarm_name = "${var.env}-deployment-controller-cpu-alarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "2"
  metric_name = "CPUUtilization"
  namespace = "AWS/EC2"
  period = "300"
  statistic = "Average"
  threshold = "90.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    InstanceId = var.controller_server_id
  }
}

resource "aws_cloudwatch_metric_alarm" "controller_status_check_alarm" {
  alarm_name = "${var.env}-deployment-controller-status-check-alarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "StatusCheckFailed"
  namespace = "AWS/EC2"
  period = "180"
  statistic = "Maximum"
  threshold = "1.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    InstanceId = var.controller_server_id
  }
}

resource "aws_cloudwatch_metric_alarm" "high_db_connections" {
  alarm_name = "awsrds-${var.db_name}-high-db-connections"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "DatabaseConnections"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "10000.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_queue_depth" {
  alarm_name = "awsrds-${var.db_name}-db-high-queue-depth"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "DiskQueueDepth"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "4.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_iops" {
  alarm_name = "awsrds-${var.db_name}-db-high-read-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "ReadIOPS"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "800.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_latency" {
  alarm_name = "awsrds-${var.db_name}-db-high-read-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "3"
  metric_name = "ReadLatency"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "0.004"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_throughput" {
  alarm_name = "awsrds-${var.db_name}-db-high-read-throughput"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "ReadThroughput"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "10485760.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_iops" {
  alarm_name = "awsrds-${var.db_name}-db-high-write-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "5"
  metric_name = "WriteIOPS"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "1500.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_very_high_write_iops" {
  alarm_name = "awsrds-${var.db_name}-db-very-high-write-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "WriteIOPS"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "4500.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_latency" {
  alarm_name = "awsrds-${var.db_name}-db-high-write-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "3"
  metric_name = "WriteLatency"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "0.04"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_throughput" {
  alarm_name = "awsrds-${var.db_name}-db-high-write-throughput"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "WriteThroughput"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "41943040.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "app_elb_http_code_elb_4xx_count" {
  alarm_name = "${var.env}-app-elb-http-code-elb_4xx_count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "HTTPCode_ELB_4XX_Count"
  namespace = "AWS/Application­ELB"
  period = "60"
  statistic = "Average"
  threshold = "10.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    LoadBalancer = var.loadbalancer_arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "http_code_elb_5xx_count" {
  alarm_name = "${var.env}-app-elb-http-code_elb-5xx_count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "2"
  metric_name = "HTTPCode_ELB_5XX_Count"
  namespace = "AWS/Application­ELB"
  period = "60"
  statistic = "Average"
  threshold = "10.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    LoadBalancer = var.loadbalancer_arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "postgres_transaction_logs_disk_usage" {
  alarm_name = "${var.db_name}-postgres-transaction-logs-disk-usage"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "15"
  metric_name = "TransactionLogsDiskUsage"
  namespace = "AWS/RDS"
  period = "60"
  statistic = "Average"
  threshold = "3000000000.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_cpu_utilization" {
  alarm_name = "${var.db_name}-db-cpu-utilization"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "10"
  metric_name = "CPUUtilization"
  namespace = "AWS/RDS"
  period = "60"
  statistic = "Average"
  threshold = "70.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_free_storage_space" {
  alarm_name = "${var.db_name}-db-free-storage-space"
  comparison_operator = "LessThanThreshold"
  evaluation_periods = "1"
  metric_name = "FreeStorageSpace"
  namespace = "AWS/RDS"
  period = "3600"
  statistic = "Average"
  threshold = "100000000000.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}

resource "aws_cloudwatch_metric_alarm" "db_swap_usage" {
  alarm_name = "${var.db_name}-db-swap-usage"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = "1"
  metric_name = "SwapUsage"
  namespace = "AWS/RDS"
  period = "300"
  statistic = "Average"
  threshold = "1.0"
  # alarm_actions = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = var.db_name
  }
}