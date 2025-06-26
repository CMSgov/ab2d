resource "aws_security_group" "this" {
  name        = "${local.app}-${local.env}-quicksight-rds"
  description = "Security Group for QuickSight RDS Connection"
  vpc_id      = local.vpc_id
}

data "aws_security_group" "db" {
  filter {
    name   = "tag:Name"
    values = ["${local.service_prefix}-db"]
  }
}

resource "aws_security_group_rule" "db_ingress" {
  type                     = "ingress"
  description              = "QuickSight RDS Inbound Access"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.this.id
  security_group_id        = data.aws_security_group.db.id
}

resource "aws_security_group_rule" "quicksight_egress" {
  type                     = "egress"
  description              = "QuickSight Outbound Access"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.db.id
  security_group_id        = aws_security_group.this.id
}

resource "aws_quicksight_vpc_connection" "this" {
  name               = "${local.app}-${local.env}"
  security_group_ids = [aws_security_group.this.id]
  subnet_ids         = local.private_subnet_ids
  vpc_connection_id  = "${local.app}-${local.env}"
  role_arn           = aws_iam_role.this.arn
}
