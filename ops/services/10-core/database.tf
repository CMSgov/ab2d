module "db" {
  source = "../../tf-modules/ds"

  snapshot          = var.snapshot
  platform          = module.platform
  username          = local.database_user
  password          = local.database_password
  iops              = local.parent_env == "prod" ? 20000 : 5000                      #FIXME Challenge this assumption
  instance_class    = local.parent_env == "prod" ? "db.m6g.2xlarge" : "db.m6g.large" #FIXME Challenge this assumption
  storage_type      = "io1"                                                          #FIXME Challenge this assumption
  allocated_storage = 500                                                            #FIXME Challenge this assumption
  multi_az          = local.parent_env == "prod" ? true : false

  backup_window = lookup({
    dev     = "08:06-08:36"
    test    = "08:06-08:36"
    sandbox = "08:06-08:36"
    prod    = "03:15-03:45"
  }, local.parent_env, "00:00-00:30")

  maintenance_window = lookup({
    dev     = "sun:08:52-sun:09:22"
    test    = "sun:08:52-sun:09:22"
    sandbox = "sun:08:52-sun:09:22"
    prod    = "tue:20:00-tue:20:30"
  }, local.parent_env, "mon:00:00-mon:00:30")

  monitoring_interval = lookup({
    prod = 60
  }, local.parent_env, 0)

  performance_insights_enabled = lookup({
    prod = true
  }, local.parent_env, false)

  performance_insights_retention_period = lookup({
    prod = 7
  }, local.parent_env, 0)
  deletion_protection = !module.platform.is_ephemeral_env
}

resource "aws_security_group_rule" "db_access_api" {
  type                     = "ingress"
  description              = "${local.service_prefix} api connections"
  from_port                = "5432"
  to_port                  = "5432"
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.api.id
  security_group_id        = module.db.sg.id
}

resource "aws_cloudwatch_metric_alarm" "high_db_connections" {
  alarm_name          = "${local.service_prefix}-awsrds-high-db-connections"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "10000.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_queue_depth" {
  alarm_name          = "${local.service_prefix}-awsrds-db-high-queue-depth"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "DiskQueueDepth"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "4.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_iops" {
  alarm_name          = "${local.service_prefix}-awsrds-db-high-read-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "ReadIOPS"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1700.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_latency" {
  alarm_name          = "${local.service_prefix}-awsrds-db-high-read-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "3"
  metric_name         = "ReadLatency"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "0.006"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_throughput" {
  alarm_name          = "${local.service_prefix}-awsrds-db-high-read-throughput"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "ReadThroughput"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "40000000.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_iops" {
  alarm_name          = "${local.service_prefix}-awsrds-db-high-write-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "5"
  metric_name         = "WriteIOPS"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1700.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_very_high_write_iops" {
  alarm_name          = "${local.service_prefix}-awsrds-db-very-high-write-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "WriteIOPS"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "4500.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_latency" {
  alarm_name          = "${local.service_prefix}-awsrds-db-high-write-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "3"
  metric_name         = "WriteLatency"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "0.04"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_throughput" {
  alarm_name          = "${local.service_prefix}-awsrds-db-high-write-throughput"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "WriteThroughput"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "41943040.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "postgres_transaction_logs_disk_usage" {
  alarm_name          = "${local.service_prefix}-postgres-transaction-logs-disk-usage"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "15"
  metric_name         = "TransactionLogsDiskUsage"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "4000000000.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_cpu_utilization" {
  alarm_name          = "${local.service_prefix}-db-cpu-utilization"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "10"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "90.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_free_storage_space" {
  alarm_name          = "${local.service_prefix}-db-free-storage-space"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = "3600"
  statistic           = "Average"
  threshold           = "100000000000.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_swap_usage" {
  alarm_name          = "${local.service_prefix}-db-swap-usage"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "SwapUsage"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.instance.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "aurora_cpu_utilization" {
  alarm_name          = "${local.service_prefix}-aurora-cpu-utilization"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "10"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "90.0"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  dimensions = {
    DBInstanceIdentifier = module.db.aurora_instance[0].identifier
  }
}

resource "aws_ssm_parameter" "db_endpoint" {
  name  = "/ab2d/${local.env}/core/nonsensitive/database_endpoint"
  value = module.db.instance.endpoint
  type  = "String"
}
