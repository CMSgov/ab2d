locals {
  service_prefix = "${var.platform.app}-${var.platform.env}"
}

resource "aws_db_instance" "this" {
  username                   = var.username
  password                   = var.password
  snapshot_identifier        = var.snapshot
  allocated_storage          = 500 #TODO
  apply_immediately          = true
  auto_minor_version_upgrade = true
  availability_zone          = "us-east-1a" #TODO
  backup_retention_period    = 7
  backup_target              = "region"
  backup_window              = "08:06-08:36" #TODO
  ca_cert_identifier         = "rds-ca-rsa2048-g1"
  copy_tags_to_snapshot      = true
  database_insights_mode     = "standard" #TODO
  db_subnet_group_name       = aws_db_subnet_group.this.name
  enabled_cloudwatch_logs_exports = [
    "postgresql",
    "upgrade",
  ]
  engine                                = "postgres"
  engine_version                        = 16.8  #TODO
  iam_database_authentication_enabled   = false #TODO
  identifier                            = local.service_prefix
  instance_class                        = "db.m6g.large" #TODO
  iops                                  = 5000           #TODO
  kms_key_id                            = var.platform.kms_alias_primary.target_key_arn
  storage_encrypted                     = true
  maintenance_window                    = "sun:08:52-sun:09:22" #TODO
  max_allocated_storage                 = 0                     #TODO
  monitoring_interval                   = 0                     #TODO
  monitoring_role_arn                   = null                  #TODO
  multi_az                              = false                 #TODO
  nchar_character_set_name              = null
  network_type                          = "IPV4"
  parameter_group_name                  = aws_db_parameter_group.this.name
  performance_insights_enabled          = false #TODO
  performance_insights_kms_key_id       = null
  performance_insights_retention_period = 0     #TODO
  port                                  = 5432  #TODO
  skip_final_snapshot                   = true  #TODO
  storage_type                          = "io1" #TODO
  tags = {
    "AWS_Backup" = "4hr7_w90" #TODO
  }
  vpc_security_group_ids = [
    aws_security_group.this.id,
    var.platform.security_groups["cmscloud-security-tools"].id,
    var.platform.security_groups["remote-management"].id,
    var.platform.security_groups["zscaler-private"].id
  ]
}

resource "aws_db_parameter_group" "this" {
  description  = "Managed by Terraform"
  family       = "postgres16"
  name         = "${local.service_prefix}-rds-parameter-group-v16"
  skip_destroy = false #TODO

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

resource "aws_db_subnet_group" "this" {
  description = "Managed by Terraform"
  name        = local.service_prefix
  subnet_ids  = keys(var.platform.private_subnets)
}

resource "aws_security_group" "this" {
  description            = "${local.service_prefix} database security group"
  name                   = "${local.service_prefix}-db"
  revoke_rules_on_delete = false #TODO
  tags = {
    Name = "${local.service_prefix}-db"
  }
  vpc_id = var.platform.vpc_id
}

resource "aws_vpc_security_group_egress_rule" "this" {
  cidr_ipv4         = "0.0.0.0/0"
  description       = "Allow all egress"
  ip_protocol       = "-1"
  security_group_id = aws_security_group.this.id
}
