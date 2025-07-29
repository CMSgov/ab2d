resource "aws_rds_cluster" "this" {
  count = var.create_aurora_cluster ? 1 : 0

  cluster_identifier              = coalesce(var.cluster_identifier, local.service_prefix)
  engine                          = local.aurora_engine
  engine_version                  = var.engine_version
  master_username                 = var.username
  master_password                 = var.password
  snapshot_identifier             = var.aurora_snapshot
  db_subnet_group_name            = aws_db_subnet_group.this.name
  storage_type                    = var.aurora_storage_type
  storage_encrypted               = true
  kms_key_id                      = coalesce(var.kms_key_override, var.platform.kms_alias_primary.target_key_arn)
  backup_retention_period         = var.backup_retention_period
  preferred_backup_window         = var.backup_window
  preferred_maintenance_window    = var.maintenance_window
  apply_immediately               = false
  skip_final_snapshot             = var.platform.is_ephemeral_env ? true : false
  deletion_protection             = var.deletion_protection
  db_cluster_parameter_group_name = aws_rds_cluster_parameter_group.this[0].name
  vpc_security_group_ids = flatten([
    aws_security_group.this.id,
    var.platform.security_groups.cmscloud-security-tools.id,
    var.platform.security_groups.remote-management.id,
    var.platform.security_groups.zscaler-private.id,
    var.vpc_security_group_ids
  ])
  tags = {
    AWS_Backup = "4hr7_w90"
  }

  provisioner "local-exec" {
    environment = {
      DB_CLUSTER_ID                    = self.cluster_identifier
      KMS_KEY_ID                       = self.kms_key_id
      ENHANCED_MONITORING_INTERVAL     = var.monitoring_interval
      ENHANCED_MONITORING_IAM_ROLE_ARN = var.monitoring_role_arn
    }
    command     = <<-EOF
    aws rds modify-db-cluster --db-cluster-identifier "$DB_CLUSTER_ID" \
      --performance-insights-kms-key-id "$KMS_KEY_ID" \
      --enable-performance-insights \
      --monitoring-interval "$ENHANCED_MONITORING_INTERVAL" \
      --monitoring-role-arn "$ENHANCED_MONITORING_IAM_ROLE_ARN" 1>/dev/null &&
      echo "Performance Insights and Enhanced Monitoring enabled for $DB_CLUSTER_ID"
    EOF
    interpreter = ["/bin/bash", "-c"]
  }

  lifecycle {
    ignore_changes = [
      monitoring_interval,
      monitoring_role_arn,
      performance_insights_enabled,
      performance_insights_kms_key_id,
      performance_insights_retention_period,
    ]
  }
}

resource "aws_rds_cluster_instance" "this" {
  count = var.create_aurora_cluster ? 1 : 0

  identifier                 = "${coalesce(var.cluster_identifier, local.service_prefix)}-0"
  cluster_identifier         = aws_rds_cluster.this[0].id
  engine                     = aws_rds_cluster.this[0].engine
  engine_version             = aws_rds_cluster.this[0].engine_version
  db_subnet_group_name       = aws_db_subnet_group.this.name
  instance_class             = var.aurora_instance_class
  publicly_accessible        = false
  apply_immediately          = true
  auto_minor_version_upgrade = true
  db_parameter_group_name    = aws_db_parameter_group.aurora[0].name
  tags = {
    Name = "${local.service_prefix}-0"
  }

  lifecycle {
    ignore_changes = [
      monitoring_interval,
      monitoring_role_arn,
      performance_insights_enabled,
      performance_insights_kms_key_id,
      performance_insights_retention_period,
    ]
  }
}

resource "aws_rds_cluster_parameter_group" "this" {
  count = var.create_aurora_cluster ? 1 : 0

  name        = "${local.service_prefix}-cluster"
  family      = local.aurora_family
  description = "Aurora cluster parameter group for ${local.service_prefix}"

  dynamic "parameter" {
    for_each = toset(var.aurora_cluster_parameters)

    content {
      apply_method = parameter.value.apply_method
      name         = parameter.value.name
      value        = parameter.value.value
    }
  }
}

resource "aws_db_parameter_group" "aurora" {
  count = var.create_aurora_cluster ? 1 : 0

  name        = "${local.service_prefix}-instance"
  family      = local.aurora_family
  description = "Aurora DB instance parameter group for ${local.service_prefix}"

  dynamic "parameter" {
    for_each = toset(var.aurora_cluster_instance_parameters)

    content {
      apply_method = parameter.value.apply_method
      name         = parameter.value.name
      value        = parameter.value.value
    }
  }
}
