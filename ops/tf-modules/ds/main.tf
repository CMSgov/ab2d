locals {
  service_prefix = "${var.platform.app}-${var.platform.env}"
  major_version  = split(".", var.engine_version)[0]
  aurora_engine  = "aurora-postgresql"
  aurora_family  = "${local.aurora_engine}${local.major_version}"
}

resource "aws_db_subnet_group" "this" {
  description = "Managed by Terraform"
  name        = local.service_prefix
  subnet_ids  = keys(var.platform.private_subnets)
}

resource "aws_security_group" "this" {
  description            = "${local.service_prefix} database security group"
  name                   = "${local.service_prefix}-db"
  revoke_rules_on_delete = false
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
