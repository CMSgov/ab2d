provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
  profile = var.aws_profile
}

# Had to hardcode the key since terraform says "Variables may not be used here"
terraform {
  backend "s3" {
    bucket         = "cms-ab2d-automation"
    key            = "ab2d-shared/terraform/terraform.tfstate"
    region         = "us-east-1"
    encrypt = true
  }
}

module "kms" {
  source             = "../../modules/kms"
  env                = var.env
  aws_account_number = var.aws_account_number
}

module "db" {
  source                  = "../../modules/db"
  allocated_storage_size  = var.db_allocated_storage_size
  engine_version          = var.postgres_engine_version
  instance_class          = var.db_instance_class
  snapshot_id             = var.db_snapshot_id
  subnet_group_name       = var.db_subnet_group_name
  parameter_group_name    = var.db_parameter_group_name
  backup_retention_period = var.db_backup_retention_period
  backup_window           = var.db_backup_window
  copy_tags_to_snapshot   = var.db_copy_tags_to_snapshot
  iops                    = var.db_iops
  kms_key_id              = module.kms.arn
  maintenance_window      = var.db_maintenance_window
  vpc_id                  = var.vpc_id
  db_instance_subnet_ids  = var.private_subnet_ids
  identifier              = var.db_identifier
  multi_az                = var.db_multi_az
  username                = var.db_username
  password                = var.db_password
  skip_final_snapshot     = var.db_skip_final_snapshot
}

module "s3" {
  source              = "../../modules/s3"
  env                 = var.env
  vpc_id              = var.vpc_id
  bucket_name         = var.file_bucket_name
  encryption_key_arn  = module.kms.arn
  logging_bucket_name = var.logging_bucket_name
  username_list       = var.s3_username_whitelist
}

# LSH SKIP FOR NOW BEGIN
# vpn-private-sec-group-id      = var.vpn-private-sec-group-id
# enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
# LSH SKIP FOR NOW END
module "controller" {
  source                = "../../modules/controller"
  env                   = var.env
  vpc_id                = var.vpc_id
  controller_subnet_ids = var.deployment_controller_subnet_ids
  db_sec_group_id       = module.db.aws_security_group_sg_database_id
  ami_id                = var.ami_id
  instance_type         = var.ec2_instance_type
  linux_user            = var.linux_user
  ssh_key_name          = var.ssh_key_name
  iam_instance_profile  = var.ec2_iam_profile
  gold_disk_name        = var.gold_image_name
  deployer_ip_address   = var.deployer_ip_address
}

module "lonnie_access_controller" {
  description  = "Lonnie"
  cidr_blocks  = ["${var.deployer_ip_address}/32"]
  source       = "../../modules/access_controller"
  sec_group_id = module.controller.deployment_controller_sec_group_id
}

resource "null_resource" "authorized_keys_file" {
  depends_on = [module.controller]

  provisioner "local-exec" {
    command = "scp -o StrictHostKeyChecking=no -i ~/.ssh/${var.ssh_key_name}.pem ./authorized_keys ${var.linux_user}@${module.controller.deployment_controller_public_ip}:/home/${var.linux_user}/.ssh"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${module.controller.deployment_controller_public_ip} 'chmod 600 ~/.ssh/authorized_keys'"
  }
}
