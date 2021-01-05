resource "aws_security_group" "jenkins_agent" {
  name        = "ab2d-jenkins-agent-sg"
  description = "Jenkins Agent"
  vpc_id      = var.vpc_id
  tags = {
    Name = "ab2d-jenkins-agent-sg"
  }
}

resource "aws_security_group_rule" "egress_jenkins_agent" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.jenkins_agent.id
}

resource "aws_security_group_rule" "agent_access" {
  type        = "ingress"
  description = "Agent Access"
  from_port   = "-1"
  to_port     = "-1"
  protocol    = "-1"
  source_security_group_id = aws_security_group.jenkins_agent.id
  security_group_id = var.jenkins_master_sec_group_id
}

resource "aws_security_group_rule" "master_access" {
  type        = "ingress"
  description = "Master Access"
  from_port   = "-1"
  to_port     = "-1"
  protocol    = "-1"
  source_security_group_id = var.jenkins_master_sec_group_id
  security_group_id = aws_security_group.jenkins_agent.id
}

resource "random_shuffle" "private_subnets" {
  input = var.private_subnet_ids
  result_count = 1
}

resource "aws_instance" "jenkins_agent" {
  ami = var.ami_id
  instance_type = var.instance_type
  vpc_security_group_ids = [aws_security_group.jenkins_agent.id,var.vpn_private_sec_group_id]
  disable_api_termination = false
  key_name = var.ssh_key_name
  monitoring = true
  subnet_id = random_shuffle.private_subnets.result[0]
  associate_public_ip_address = false
  iam_instance_profile = var.iam_instance_profile
  user_data = templatefile("${path.module}/userdata.tpl",{ env = "${lower(var.env)}" })

  volume_tags = {
    Name = "ab2d-jenkins-agent-vol"
  }
  
  tags = {
    Name = var.ec2_instance_tag
    application = "ab2d"
    stack = "shared"
    purpose = "ECS container instance"
    sensitivity = "Public"
    maintainer = "lonnie.hanekamp@semanticbits.com"
    "cpm backup" = "4HR Daily Weekly Monthly"
    purchase_type = "On-Demand"
    os_license = "Red Hat Enterprise Linux"
    business = "CMS"
  }
}

resource "null_resource" "wait" {

  depends_on = ["aws_instance.jenkins_agent"]
  triggers = {jenkins_agent_id = aws_instance.jenkins_agent.id}

  provisioner "local-exec" {
    command = "sleep 120"
  }
  
}