provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
  profile = var.aws_profile
}

# Had to pass "-backend-config" parameters to "terraform init" since "Variables
# may not be used here"
terraform {
  backend "s3" {
  }
}

module "kms" {
  source             = "../../modules/kms"
  env                = var.env
  aws_account_number = var.aws_account_number
}

module "jenkins_master" {
  source                   = "../../modules/jenkins_master"
  env                      = var.env
  vpc_id                   = var.vpc_id
  public_subnet_ids        = var.public_subnet_ids
  vpn_private_sec_group_id = var.vpn_private_sec_group_id
  ami_id                   = var.ami_id
  instance_type            = var.ec2_instance_type
  linux_user               = var.linux_user
  ssh_key_name             = var.ssh_key_name
  iam_instance_profile     = var.ec2_iam_profile
}

resource "null_resource" "authorized_keys_file" {
  depends_on = [module.jenkins_master]

  provisioner "local-exec" {
    command = "scp -o StrictHostKeyChecking=no -i ~/.ssh/${var.ssh_key_name}.pem ./authorized_keys ${var.linux_user}@${module.jenkins_master.jenkins_master_private_ip}:/home/${var.linux_user}/.ssh"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${module.jenkins_master.jenkins_master_private_ip} 'chmod 600 ~/.ssh/authorized_keys'"
  }
}
