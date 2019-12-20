provider "aws" {
  region  = "ca-central-1"
  version = "~> 2.21"
  profile = var.aws_profile
}

terraform {
  backend "s3" {
    bucket         = "ab2d-automation-ca-central-1"
    key            = "vpc-peering-test/terraform/terraform.tfstate"
    region         = "ca-central-1"
    encrypt = true
  }
}

module "test_controller" {
  source                  = "../../modules/ec2_public_instance"
  env                     = var.env
  vpc_id                  = var.vpc_id_1
  public_subnet_ids       = var.public_subnet_ids_env_1
  ami_id                  = var.ami_id
  instance_type           = var.ec2_instance_type
  linux_user              = var.linux_user
  ssh_key_name            = var.ssh_key_name
  iam_instance_profile    = var.ec2_iam_profile
  gold_disk_name          = var.gold_image_name
  deployer_ip_address     = var.deployer_ip_address
}

module "lonnie_access_controller" {
  description  = "Lonnie"
  cidr_blocks  = ["${var.deployer_ip_address}/32"]
  source       = "../../modules/access_controller"
  sec_group_id = module.test_controller.test_controller_sec_group_id
}

resource "null_resource" "authorized_keys_file" {
  depends_on = [module.test_controller]

  provisioner "local-exec" {
    command = "scp -o StrictHostKeyChecking=no -i ~/.ssh/${var.ssh_key_name}.pem ./authorized_keys ${var.linux_user}@${module.test_controller.test_controller_public_ip}:/home/${var.linux_user}/.ssh"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${module.test_controller.test_controller_public_ip} 'chmod 600 ~/.ssh/authorized_keys'"
  }
}

module "test_node" {
  source                       = "../../modules/ec2_private_instance"
  env                          = var.env
  vpc_id                       = var.vpc_id_2
  private_subnet_ids           = var.private_subnet_ids_env_2
  test_controller_sec_group_id = module.test_controller.test_controller_sec_group_id
  ami_id                       = var.ami_id
  instance_type                = var.ec2_instance_type
  linux_user                   = var.linux_user
  ssh_key_name                 = var.ssh_key_name
  iam_instance_profile         = var.ec2_iam_profile
  gold_disk_name               = var.gold_image_name
}
