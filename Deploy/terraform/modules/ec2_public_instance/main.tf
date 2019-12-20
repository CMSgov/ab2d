resource "aws_security_group" "test_controller" {
  name        = "ab2d-test-controller-sg"
  description = "Test Controller"
  vpc_id      = var.vpc_id
  tags = {
    Name = "ab2d-test-controller-sg"
  }
}

resource "aws_security_group_rule" "whitelist_lonnie" {
  type        = "ingress"
  description = "Whitelist Lonnie"
  from_port   = "22"
  to_port     = "22"
  protocol    = "TCP"
  cidr_blocks = ["${var.deployer_ip_address}/32"]
  security_group_id = aws_security_group.test_controller.id
}

resource "aws_security_group_rule" "egress_controller" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.test_controller.id
}

resource "random_shuffle" "public_subnets" {
  input = var.public_subnet_ids
  result_count = 1
}

resource "aws_instance" "test_controller" {
  ami = var.ami_id
  instance_type = var.instance_type
  vpc_security_group_ids = [aws_security_group.test_controller.id]
  disable_api_termination = false
  key_name = var.ssh_key_name
  monitoring = true
  subnet_id = random_shuffle.public_subnets.result[0]
  associate_public_ip_address = true
  iam_instance_profile = var.iam_instance_profile

  volume_tags = {
    Name = "ab2d-test-controller-vol"
  }
  
  tags = {
    Name = "ab2d-test-controller"
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

resource "aws_eip" "test_controller" {
  depends_on = ["aws_instance.test_controller"]
  instance = aws_instance.test_controller.id
  vpc = true
  tags = {
    Name = "ab2d-test-controller-eip"
  }
}

resource "null_resource" "wait" {

  depends_on = ["aws_instance.test_controller","aws_eip.test_controller"]
  triggers = {controller_id = aws_instance.test_controller.id}

  provisioner "local-exec" {
    command = "sleep 120"
  }
  
}

resource "null_resource" "list-api-instances-script" {

  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.test_controller.id}

  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ${path.cwd}/../../environments/ab2d-${var.env}/list-api-instances.sh ${var.linux_user}@${aws_eip.test_controller.public_ip}:/home/${var.linux_user}"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.test_controller.public_ip} 'chmod +x /home/${var.linux_user}/list-api-instances.sh'"
  }
  
}

resource "null_resource" "set-hostname" {

  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.test_controller.id}

  provisioner "local-exec" {
    command = "ssh -tt -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.test_controller.public_ip} 'echo \"ab2d-controller\" > /tmp/hostname && sudo mv /tmp/hostname /etc/hostname && sudo hostname \"ab2d-controller\"'"
  }
  
}

resource "null_resource" "test_contoller_private_key" {

  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.test_controller.id}
  
  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.test_controller.public_ip}:/tmp/id.rsa"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.test_controller.public_ip} 'chmod 600 /tmp/id.rsa && mv /tmp/id.rsa ~/.ssh/'"
  }
  
}

resource "null_resource" "ssh_client_config" {

  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.test_controller.id}
  
  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ../../environments/ab2d-${var.env}/client_config ${var.linux_user}@${aws_eip.test_controller.public_ip}:/home/${var.linux_user}/.ssh/config"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.test_controller.public_ip} 'chmod 640 /home/${var.linux_user}/.ssh/config'"
  }
  
}

resource "null_resource" "remove_docker_from_controller" {

  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.test_controller.id}
  
  provisioner "local-exec" {
    command = "ssh -tt -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.test_controller.public_ip} 'sudo yum -y remove docker-ce-*'"
  }
  
}
