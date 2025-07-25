module "db" {
  source = "../../tf-modules/ds"

  snapshot       = var.snapshot
  platform       = module.platform
  username       = local.database_user
  password       = local.database_password
  iops           = local.parent_env == "prod" ? 20000 : 5000
  instance_class = local.parent_env == "prod" ? "db.m6g.2xlarge" : "db.m6g.large"
  aurora_instance_class = lookup({
    prod = "db.r8g.2xlarge"
  }, local.parent_env, "db.r8g.large")
  storage_type        = "io1"
  allocated_storage   = 500
  multi_az            = local.parent_env == "prod" ? true : false
  monitoring_role_arn = aws_iam_role.db_monitoring.arn
  aurora_snapshot     = var.aurora_snapshot

  create_aurora_cluster  = contains(["dev", "test", "sandbox", "prod"], local.parent_env)
  create_rds_db_instance = contains(["prod"], local.parent_env)

  backup_retention_period = module.platform.is_ephemeral_env ? 1 : 7
  aurora_storage_type = lookup({
    prod = "aurora-iopt1"
    dev  = "aurora-iopt1" #TODO: Remove NLT 2025-08-15
    test = "aurora-iopt1" #TODO: Remove NLT 2025-08-15
  }, local.env, "")

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
    prod = 15
  }, local.parent_env, 60)

  deletion_protection = !module.platform.is_ephemeral_env

  aurora_cluster_parameters = [
    {
      apply_method = "immediate"
      name         = "rds.force_ssl"
      value        = 1
    },
    {
      apply_method = "immediate"
      name         = "backslash_quote"
      value        = "safe_encoding"
    },
  ]
  aurora_cluster_instance_parameters = [
    {
      apply_method = "immediate"
      name         = "random_page_cost"
      value        = "1.1"
    },
    {
      apply_method = "immediate"
      name         = "work_mem"
      value        = "32768"
    },
    {
      apply_method = "immediate"
      name         = "statement_timeout"
      value        = "1200000"
    },
    {
      apply_method = "pending-reboot"
      name         = "cron.database_name"
      value        = local.benv
    },
    {
      apply_method = "pending-reboot"
      name         = "shared_preload_libraries"
      value        = "pg_stat_statements,pg_cron"
    }
  ]
}

resource "aws_security_group_rule" "db_access_api" {
  type                     = "ingress"
  description              = "${local.service_prefix} api connections"
  from_port                = "5432"
  to_port                  = "5432"
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.api.id
  security_group_id        = module.db.security_group.id
}

resource "aws_cloudwatch_metric_alarm" "high_db_connections" {
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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
  count               = contains(["prod"], local.env) ? 1 : 0
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

resource "aws_ssm_parameter" "db_endpoint" {
  count = contains(["prod"], local.parent_env) ? 1 : 0
  name  = "/ab2d/${local.env}/core/nonsensitive/database_endpoint"
  value = module.db.instance.endpoint
  type  = "String"
}

resource "aws_ssm_parameter" "writer_endpoint" {
  count = contains(["dev", "test", "sandbox"], local.parent_env) ? 1 : 0
  name  = "/ab2d/${local.env}/core/nonsensitive/writer_endpoint"
  value = "${module.db.aurora_cluster.endpoint}:${module.db.aurora_cluster.port}"
  type  = "String"
}

data "aws_ssm_parameter" "splunk_oncall_email" {
  count = var.parent_env == "prod" || var.parent_env == "sandbox" ? 1 : 0
  name  = "/ab2d/splunk_oncall/alerting/email"
}

resource "aws_sns_topic_subscription" "splunk_oncall_email" {
  count     = length(data.aws_ssm_parameter.splunk_oncall_email)
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = data.aws_ssm_parameter.splunk_oncall_email[0].value
}
