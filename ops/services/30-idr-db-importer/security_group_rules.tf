# Allow ingress into the database from this service
resource "aws_vpc_security_group_ingress_rule" "aurora" {
  security_group_id            = data.aws_ssm_parameter.ab2d_aurora_database_security_group.value
  referenced_security_group_id = module.service.task_security_group_id

  from_port   = 5432
  ip_protocol = "tcp"
  to_port     = 5432
}

resource "aws_security_group" "idr_endpoint" {
  name        = "${local.service_prefix}-idr-endpoint"
  description = "For the PrivateLink endpoint for IDR Snowflake"
  vpc_id      = module.platform.vpc_id
  tags        = { Name = "${local.service_prefix}-idr-endpoint" }
}

resource "aws_vpc_security_group_ingress_rule" "idr_endpoint_http" {
  security_group_id            = aws_security_group.idr_endpoint.id
  referenced_security_group_id = module.service.task_security_group_id

  from_port   = 80
  ip_protocol = "tcp"
  to_port     = 80
}

resource "aws_vpc_security_group_ingress_rule" "idr_endpoint_https" {
  security_group_id            = aws_security_group.idr_endpoint.id
  referenced_security_group_id = module.service.task_security_group_id

  from_port   = 443
  ip_protocol = "tcp"
  to_port     = 443
}
