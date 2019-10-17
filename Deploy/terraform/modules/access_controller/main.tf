resource "aws_security_group_rule" "ssh" {
  type        = "ingress"
  description = var.description
  from_port   = "22"
  to_port     = "22"
  protocol    = "tcp"
  cidr_blocks = var.cidr_blocks
  security_group_id = var.sec_group_id
}
