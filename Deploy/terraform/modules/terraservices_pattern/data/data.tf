# Create database security group

resource "aws_security_group" "sg_database" {
  name        = "${var.env}-database-sg"
  description = "${var.env} database security group"
  vpc_id      = var.vpc_id
  tags = {
    Name = "${var.env}-database-sg"
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

resource "aws_security_group_rule" "db_access_from_controller" {
  type        = "ingress"
  description = "Controller Access"
  from_port   = "5432"
  to_port     = "5432"
  protocol    = "tcp"
  source_security_group_id = var.controller_sg_id
  security_group_id = aws_security_group.sg_database.id
}

# Create database subnet group

resource "aws_db_subnet_group" "subnet_group" {
  name = var.db_subnet_group_name
  subnet_ids = [var.private_subnet_a_id,var.private_subnet_b_id]
}

# Create database parameter group

resource "aws_db_parameter_group" "parameter_group" {
  name   = var.db_parameter_group_name
  family = "postgres11"

  parameter {
    name         = "backslash_quote"
    value        = "safe_encoding"
    apply_method = "immediate"
  }
}

# Create database instance

resource "aws_db_instance" "db" {
  allocated_storage               = var.db_allocated_storage_size
  engine                          = "postgres"
  engine_version                  = var.postgres_engine_version
  instance_class                  = var.db_instance_class
  identifier                      = var.db_identifier
  snapshot_identifier             = var.db_snapshot_id
  db_subnet_group_name            = aws_db_subnet_group.subnet_group.name
  parameter_group_name            = aws_db_parameter_group.parameter_group.name
  backup_retention_period         = var.db_backup_retention_period
  backup_window                   = var.db_backup_window
  copy_tags_to_snapshot           = var.db_copy_tags_to_snapshot
  iops                            = var.db_iops
  apply_immediately               = true
  kms_key_id                      = var.main_kms_key_arn
  maintenance_window              = var.db_maintenance_window
  multi_az                        = var.db_multi_az
  storage_encrypted               = true
  vpc_security_group_ids          = [aws_security_group.sg_database.id]
  username                        = var.db_username
  password                        = var.db_password
  skip_final_snapshot             = true
  deletion_protection             = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = {
    Name         = "${var.env}-rds"
    env          = var.env
    role         = "db"
    "cpm backup" = var.cpm_backup_db
  }
}
