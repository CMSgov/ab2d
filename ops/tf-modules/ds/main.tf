locals {
  service_prefix = "${var.platform.app}-${var.platform.env}"
}

resource "aws_db_instance" "this" {
  username                   = var.username
  password                   = var.password
  snapshot_identifier        = var.snapshot
  allocated_storage          = var.allocated_storage
  apply_immediately          = true
  auto_minor_version_upgrade = true
  availability_zone          = var.multi_az ? null : "us-east-1a" #TODO
  backup_retention_period    = 7
  backup_target              = "region"
  backup_window              = var.backup_window
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
  instance_class                        = var.instance_class
  iops                                  = var.iops
  kms_key_id                            = coalesce(var.kms_key_override, var.platform.kms_alias_primary.target_key_arn)
  storage_encrypted                     = true
  maintenance_window                    = var.maintenance_window
  max_allocated_storage                 = 0    #TODO
  monitoring_interval                   = var.monitoring_interval   #TODO
  monitoring_role_arn                   = null #TODO
  multi_az                              = var.multi_az
  nchar_character_set_name              = null
  network_type                          = "IPV4"
  parameter_group_name                  = aws_db_parameter_group.this.name
  performance_insights_enabled          = true  #TODO
  performance_insights_kms_key_id       = null  #TODO
  performance_insights_retention_period = 7     #TODO
  port                                  = 5432  #TODO
  skip_final_snapshot                   = true  #TODO
  storage_type                          = var.storage_type
  deletion_protection                   = var.deletion_protection
  tags = {
    AWS_Backup = "4hr7_w90"
  }
  vpc_security_group_ids = flatten([
    aws_security_group.this.id,
    var.platform.security_groups["cmscloud-security-tools"].id,
    var.platform.security_groups["remote-management"].id,
    var.platform.security_groups["zscaler-private"].id,
  var.vpc_security_group_ids])
}

resource "aws_db_parameter_group" "this" {
  description  = "Managed by Terraform" #TODO
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

resource "aws_db_subnet_group" "this" {
  description = "Managed by Terraform" #TODO
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

resource "aws_rds_cluster" "this" {
  cluster_identifier      = "${local.service_prefix}-aurora"
  engine                  = "aurora-postgresql"
  engine_version          = "16.8"
  master_username         = var.username
  master_password         = var.password
  db_subnet_group_name    = aws_db_subnet_group.this.name
  vpc_security_group_ids  = flatten([
    aws_security_group.this.id,
    var.platform.security_groups["cmscloud-security-tools"].id,
    var.platform.security_groups["remote-management"].id,
    var.platform.security_groups["zscaler-private"].id,
    var.vpc_security_group_ids
  ])
  storage_type            = aurora-iopt1
  storage_encrypted       = true
  kms_key_id              = coalesce(var.kms_key_override, var.platform.kms_alias_primary.target_key_arn)
  backup_retention_period = 7
  preferred_backup_window = var.backup_window
  apply_immediately       = true
  skip_final_snapshot     = true
  deletion_protection     = var.deletion_protection
  tags = {
    AWS_Backup = "4hr7_w90"
  }
}

resource "aws_rds_cluster_instance" "this" {
  identifier              = "${local.service_prefix}-aurora-instance-0"
  cluster_identifier      = aws_rds_cluster.this.id
  instance_class          = var.instance_class
  engine                  = aws_rds_cluster.this.engine
  engine_version          = aws_rds_cluster.this.engine_version
  db_subnet_group_name    = aws_db_subnet_group.this.name
  publicly_accessible     = false
  monitoring_interval     = var.monitoring_interval
  apply_immediately       = true
  auto_minor_version_upgrade = true
  performance_insights_enabled = true
  performance_insights_kms_key_id = null
  monitoring_role_arn     = null
  tags = {
    Name = "${local.service_prefix}-aurora-instance"
  }
}
