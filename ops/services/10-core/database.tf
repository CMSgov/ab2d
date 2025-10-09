module "db" {
  source = "github.com/CMSgov/cdap//terraform/modules/aurora?ref=bee1120c5847b617b7b9dca4cfeaaf47aa08c156"

  snapshot_identifier     = var.aurora_snapshot
  backup_retention_period = module.platform.is_ephemeral_env ? 1 : 7
  deletion_protection     = !module.platform.is_ephemeral_env
  monitoring_role_arn     = aws_iam_role.db_monitoring.arn
  password                = local.database_password
  platform                = module.platform
  username                = local.database_user

  instance_count = lookup({
    prod = "2"
  }, local.parent_env, "1")

  instance_class = lookup({
    prod = "db.r8g.xlarge"
  }, local.parent_env, "db.r8g.large")

  storage_type = lookup({
    prod = "aurora-iopt1"
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

  cluster_parameters = [
    {
      apply_method = "immediate"
      name         = "backslash_quote"
      value        = "safe_encoding"
    },
  ]
  cluster_instance_parameters = [
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
      value        = "main"
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

resource "aws_ssm_parameter" "writer_endpoint" {
  name  = "/ab2d/${local.env}/core/nonsensitive/writer_endpoint"
  value = "${module.db.aurora_cluster.endpoint}:${module.db.aurora_cluster.port}"
  type  = "String"
}
