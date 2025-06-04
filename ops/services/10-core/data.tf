data "aws_security_group" "db" {
  tags = {
    "Name" = "${local.service_prefix}-db"
  }
  vpc_id = local.vpc_id
}

data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}
