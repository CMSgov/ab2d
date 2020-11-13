data "aws_vpc" "target_vpc" {
  filter {
    name   = "tag:Name"
    values = [var.parent_env]
  }
}

resource "aws_security_group" "sg_database" {
  name        = "${var.env}-database-sg"
  description = "${var.env} database security group"
  vpc_id      = data.aws_vpc.target_vpc.id
  tags = {
    Name = "${var.env}-database-sg"
  }
}
