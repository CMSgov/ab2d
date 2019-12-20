resource "aws_security_group" "test_node" {
  name        = "ab2d-${lower(var.env)}-node-sg"
  description = "API security group"
  vpc_id      = var.vpc_id
  tags = {
    Name = "ab2d-${lower(var.env)}-api-sg"
  }
}

resource "aws_security_group_rule" "node_access" {
  type        = "ingress"
  description = "Node Access"
  from_port   = "-1"
  to_port     = "-1"
  protocol    = "-1"
  source_security_group_id = aws_security_group.test_node.id
  security_group_id = var.test_controller_sec_group_id
}

resource "aws_security_group_rule" "egress_controller" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.test_node.id
}

resource "random_shuffle" "private_subnets" {
  input = var.private_subnet_ids
  result_count = 1
}

resource "aws_instance" "test_node" {
  ami = var.ami_id
  instance_type = var.instance_type
  vpc_security_group_ids = [aws_security_group.test_node.id]
  disable_api_termination = false
  key_name = var.ssh_key_name
  monitoring = true
  subnet_id = random_shuffle.private_subnets.result[0]
  associate_public_ip_address = true
  iam_instance_profile = var.iam_instance_profile

  volume_tags = {
    Name = "ab2d-test-node-vol"
  }
  
  tags = {
    Name = "ab2d-test-node"
    application = "ab2d"
    stack = "shared"
    purpose = "ECS container instance"
    sensitivity = "Public"
    maintainer = "lonnie.hanekamp@semanticbits.com"
    cpm_backup = "NoBackup"
    purchase_type = "On-Demand"
    os_license = "Red Hat Enterprise Linux"
    gold_disk_name = var.gold_disk_name
    business = "CMS"
  }

}
