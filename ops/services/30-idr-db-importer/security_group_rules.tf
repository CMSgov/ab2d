# Allow ingress into the database from this service
resource "aws_vpc_security_group_ingress_rule" "aurora" {
  security_group_id            = data.aws_ssm_parameter.ab2d_aurora_database_security_group.value
  referenced_security_group_id = module.service.task_security_group_id

  from_port   = 5432
  ip_protocol = "tcp"
  to_port     = 5432
}
