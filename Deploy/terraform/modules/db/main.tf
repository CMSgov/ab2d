resource "aws_security_group" "sg_database" {
  name        = "ab2d-database-sg"
  description = "Database security group"
  vpc_id      = var.vpc_id
  tags = {
    Name = "ab2d-database-sg"
  }
}

resource "aws_security_group_rule" "egress" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.sg_database.id
}

resource "aws_security_group_rule" "db_access_from_jenkins_agent" {
  type        = "ingress"
  description = "Jenkins Agent Access"
  from_port   = "5432"
  to_port     = "5432"
  protocol    = "tcp"
  source_security_group_id = var.jenkins_agent_sec_group_id
  security_group_id = aws_security_group.sg_database.id
}

resource "aws_db_subnet_group" "subnet_group" {
  name = var.subnet_group_name
  subnet_ids = var.db_instance_subnet_ids
}

resource "aws_db_parameter_group" "default" {
  name = var.parameter_group_name
  family = "postgres11"

  parameter {
    name = "backslash_quote"
    value = "safe_encoding"
    apply_method = "immediate"
  }
}

resource "aws_db_instance" "db" {
  allocated_storage               = var.allocated_storage_size
  engine                          = "postgres"
  engine_version                  = var.engine_version
  instance_class                  = var.instance_class
  identifier                      = var.identifier
  snapshot_identifier             = var.snapshot_id
  db_subnet_group_name            = aws_db_subnet_group.subnet_group.id
  parameter_group_name            = aws_db_parameter_group.default.name
  backup_retention_period         = var.backup_retention_period
  backup_window                   = var.backup_window
  copy_tags_to_snapshot           = var.copy_tags_to_snapshot
  iops                            = var.iops
  apply_immediately               = true
  kms_key_id                      = var.kms_key_id
  maintenance_window              = var.maintenance_window
  multi_az                        = var.multi_az
  storage_encrypted               = true
  vpc_security_group_ids          = [aws_security_group.sg_database.id]
  username                        = var.username
  password                        = var.password
  skip_final_snapshot             = var.skip_final_snapshot
  deletion_protection             = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = {
    Name         = "${var.env}-rds"
    env          = "${var.env}"
    role         = "db"
    "cpm backup" = "${var.cpm_backup_db}"
  }
}
