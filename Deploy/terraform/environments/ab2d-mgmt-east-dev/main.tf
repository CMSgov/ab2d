provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
}

# Had to pass "-backend-config" parameters to "terraform init" since "Variables
# may not be used here"
terraform {
  backend "s3" {
  }
}

data "aws_security_group" "ab2d_jenkins_master_sg" {
  filter {
    name   = "tag:Name"
    values = ["ab2d-jenkins-master-sg"]
  }
}

module "management_account" {
  source                             = "../../modules/management_account"
  env                                = var.env
  mgmt_aws_account_number            = var.mgmt_aws_account_number
  aws_account_number                 = var.aws_account_number
  mgmt_target_aws_account_mgmt_roles = var.mgmt_target_aws_account_mgmt_roles
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

module "jenkins_agent" {
  source                       = "../../modules/jenkins_agent"
  env                          = var.env
  vpc_id                       = var.vpc_id
  public_subnet_ids            = var.public_subnet_ids
  private_subnet_ids           = var.private_subnet_ids
  jenkins_master_sec_group_id  = "${data.aws_security_group.ab2d_jenkins_master_sg.id}"
  vpn_private_sec_group_id     = var.vpn_private_sec_group_id
  ami_id                       = var.ami_id
  instance_type                = var.ec2_instance_type
  linux_user                   = var.linux_user
  ssh_key_name                 = var.ssh_key_name
  iam_instance_profile         = var.ec2_iam_profile
  ec2_instance_tag             = var.ec2_instance_tag
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
