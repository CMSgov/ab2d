resource "aws_security_group" "jenkins_master" {
  name        = "ab2d-jenkins-master-sg"
  description = "Jenkins Master"
  vpc_id      = var.vpc_id
  tags = {
    Name = "ab2d-jenkins-master-sg"
  }
}

resource "aws_security_group_rule" "egress_jenkins_master" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.jenkins_master.id
}

resource "random_shuffle" "public_subnets" {
  input = var.public_subnet_ids
  result_count = 1
}

resource "aws_instance" "jenkins_master" {
  ami = var.ami_id
  instance_type = var.instance_type
  vpc_security_group_ids = [aws_security_group.jenkins_master.id,var.vpn_private_sec_group_id]
  disable_api_termination = false
  key_name = var.ssh_key_name
  monitoring = true
  subnet_id = random_shuffle.public_subnets.result[0]
  associate_public_ip_address = true
  iam_instance_profile = var.iam_instance_profile

  volume_tags = {
    Name = "ab2d-jenkins-master-vol"
  }
  
  tags = {
    Name = "ab2d-jenkins-master"
    application = "ab2d"
    stack = "shared"
    purpose = "ECS container instance"
    sensitivity = "Public"
    maintainer = "lonnie.hanekamp@semanticbits.com"
    "cpm backup" = "Monthly"
    purchase_type = "On-Demand"
    os_license = "Red Hat Enterprise Linux"
    business = "CMS"
  }

}

resource "aws_eip" "jenkins_master" {
  depends_on = ["aws_instance.jenkins_master"]
  instance = aws_instance.jenkins_master.id
  vpc = true
  tags = {
    Name = "ab2d-jenkins-master-eip"
  }
}

resource "null_resource" "wait" {

  depends_on = ["aws_instance.jenkins_master","aws_eip.jenkins_master"]
  triggers = {jenkins_master_id = aws_instance.jenkins_master.id}

  provisioner "local-exec" {
    command = "sleep 120"
  }
  
}

resource "null_resource" "set-hostname" {

  depends_on = ["null_resource.wait"]
  triggers = {jenkins_master_id = aws_instance.jenkins_master.id}

  provisioner "local-exec" {
    command = "ssh -tt -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.jenkins_master.private_ip} 'echo \"ab2d-jenkins-master\" > /tmp/hostname && sudo mv /tmp/hostname /etc/hostname && sudo hostname \"ab2d-jenkins-master\"'"
  }
  
}

resource "null_resource" "jenkins_master_private_key" {

  depends_on = ["null_resource.wait"]
  triggers = {jenkins_master_id = aws_instance.jenkins_master.id}
  
  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.jenkins_master.private_ip}:/tmp/id.rsa"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.jenkins_master.private_ip} 'chmod 600 /tmp/id.rsa && mv /tmp/id.rsa ~/.ssh/'"
  }
  
}

resource "null_resource" "ssh_client_config" {

  depends_on = ["null_resource.wait"]
  triggers = {jenkins_master_id = aws_instance.jenkins_master.id}
  
  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ../../environments/${var.env}/client_config ${var.linux_user}@${aws_eip.jenkins_master.private_ip}:/home/${var.linux_user}/.ssh/config"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.jenkins_master.private_ip} 'chmod 640 /home/${var.linux_user}/.ssh/config'"
  }
  
}
