resource "aws_db_instance" "this" {
  count = var.create_rds_db_instance ? 1 : 0

  username                   = var.username
  password                   = var.password
  snapshot_identifier        = var.snapshot
  allocated_storage          = var.allocated_storage
  apply_immediately          = true
  auto_minor_version_upgrade = true
  availability_zone          = var.multi_az ? null : "us-east-1a"
  backup_retention_period    = 7
  backup_target              = "region"
  backup_window              = var.backup_window
  ca_cert_identifier         = "rds-ca-rsa2048-g1"
  copy_tags_to_snapshot      = true
  database_insights_mode     = "standard"
  db_subnet_group_name       = aws_db_subnet_group.this.name
  enabled_cloudwatch_logs_exports = [
    "postgresql",
    "upgrade",
  ]
  engine                                = "postgres"
  engine_version                        = var.engine_version
  iam_database_authentication_enabled   = false
  identifier                            = local.service_prefix
  instance_class                        = var.instance_class
  iops                                  = var.iops
  kms_key_id                            = coalesce(var.kms_key_override, var.platform.kms_alias_primary.target_key_arn)
  storage_encrypted                     = true
  maintenance_window                    = var.maintenance_window
  max_allocated_storage                 = 0
  monitoring_interval                   = var.monitoring_interval
  monitoring_role_arn                   = null
  multi_az                              = var.multi_az
  nchar_character_set_name              = null
  network_type                          = "IPV4"
  parameter_group_name                  = aws_db_parameter_group.this[0].name
  performance_insights_enabled          = true
  performance_insights_kms_key_id       = null
  performance_insights_retention_period = 7
  port                                  = 5432
  skip_final_snapshot                   = true
  storage_type                          = var.storage_type
  deletion_protection                   = var.deletion_protection
  tags = {
    AWS_Backup = "4hr7_w90"
  }
  vpc_security_group_ids = flatten([
    aws_security_group.this.id,
    var.platform.security_groups.cmscloud-security-tools.id,
    var.platform.security_groups.remote-management.id,
    var.platform.security_groups.zscaler-private.id,
    var.vpc_security_group_ids
  ])

  lifecycle {
    ignore_changes = [
      monitoring_interval,
      performance_insights_enabled,
      performance_insights_kms_key_id,
      performance_insights_retention_period,
    ]
  }
}

resource "aws_db_parameter_group" "this" {
  count        = var.create_rds_db_instance ? 1 : 0
  description  = "Managed by Terraform"
  family       = "postgres16"
  name         = "${local.service_prefix}-rds-parameter-group-v16"
  skip_destroy = false

  parameter {
    apply_method = "immediate"
    name         = "backslash_quote"
    value        = "safe_encoding"
  }
  parameter {
    apply_method = "immediate"
    name         = "statement_timeout"
    value        = "1200000"
  }
  parameter {
    apply_method = "pending-reboot"
    name         = "cron.database_name"
    value        = var.platform.env
  }
  parameter {
    apply_method = "pending-reboot"
    name         = "rds.logical_replication"
    value        = "0"
  }
  parameter {
    apply_method = "pending-reboot"
    name         = "shared_preload_libraries"
    value        = "pg_stat_statements,pg_cron"
  }
}
