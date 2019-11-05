provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
  profile = var.aws_profile
}

#
# LSH *** TO DO ***: consider "dynamodb_table" attribute
#
# https://www.terraform.io/docs/backends/types/s3.html
terraform {
  backend "s3" {
    bucket         = "cms-ab2d-automation"
    key            = "cms-ab2d-sbedmo-sbx/terraform/terraform.tfstate"
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
  backup_retention_period = var.db_backup_retention_period
  backup_window           = var.db_backup_window
  copy_tags_to_snapshot   = var.db_copy_tags_to_snapshot
  iops                    = var.db_iops
  kms_key_id              = module.kms.arn
  maintenance_window      = var.db_maintenance_window
  vpc_id                  = var.vpc_id
  env                     = var.env
  db_instance_subnet_ids  = var.private_subnet_ids
  identifier              = var.db_identifier
  multi_az                = var.db_multi_az
  username                = var.db_username
  password                = var.db_password
  name                    = var.db_name
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

module "efs" {
  source              = "../../modules/efs"
  env                 = var.env
  encryption_key_arn  = module.kms.arn
}

# LSH SKIP FOR NOW BEGIN
# vpn-private-sec-group-id      = var.vpn-private-sec-group-id
# enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
# LSH SKIP FOR NOW END
module "api" {
  source                        = "../../modules/api"
  env                           = var.env
  vpc_id                        = var.vpc_id
  db_sec_group_id               = module.db.aws_security_group_sg_database_id
  controller_subnet_ids         = var.deployment_controller_subnet_ids
  ami_id                        = var.ami_id
  instance_type                 = var.ec2_instance_type
  linux_user                    = var.linux_user
  ssh_key_name                  = var.ssh_key_name
  node_subnet_ids               = var.private_subnet_ids
  logging_bucket                = var.logging_bucket_name
  healthcheck_url               = var.elb_healthcheck_url
  iam_instance_profile          = var.ec2_iam_profile
  docker_repository_url         = "${var.aws_account_number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest"
  iam_role_arn                  = "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole"
  desired_instances             = var.ec2_desired_instance_count
  min_instances                 = var.ec2_minimum_instance_count
  max_instances                 = var.ec2_maximum_instance_count
  autoscale_group_wait          = "0" #Change this later for 0 downtime deployment
  gold_disk_name                = var.gold_image_name
  override_task_definition_arn  = var.current_task_definition_arn
  aws_account_number            = var.aws_account_number
}

# LSH SKIP FOR NOW BEGIN
# vpn-private-sec-group-id      = var.vpn-private-sec-group-id
# enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
# LSH SKIP FOR NOW ENS
module "worker" {
  source                        = "../../modules/worker"
  env                           = var.env
  vpc_id                        = var.vpc_id
  ami_id                        = var.ami_id
  instance_type                 = var.ec2_instance_type
  ssh_key_name                  = var.ssh_key_name
  node_subnet_ids               = var.private_subnet_ids
  iam_instance_profile          = var.ec2_iam_profile
  docker_repository_url         = "${var.aws_account_number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest"
  desired_instances             = var.ec2_desired_instance_count
  min_instances                 = var.ec2_minimum_instance_count
  max_instances                 = var.ec2_maximum_instance_count
  autoscale_group_wait          = "0" #Change this later for 0 downtime deployment
  gold_disk_name                = var.gold_image_name
  override_task_definition_arn  = var.current_task_definition_arn
  app_sec_group_id              = module.api.application_security_group_id
  controller_sec_group_id       = module.api.deployment_controller_sec_group_id
  loadbalancer_subnet_ids       = var.deployment_controller_subnet_ids
  vpc_cidrs                     = ["10.124.1.0/24"]
  efs_id                        = module.efs.efs_id
  alpha                         = var.private_subnet_ids[0]
  beta                          = var.private_subnet_ids[1]
  ecs_cluster_id                = module.api.ecs_cluster_id
  aws_account_number            = var.aws_account_number
}

module "lonnie_access_controller" {
  description  = "Lonnie"
  cidr_blocks  = ["152.208.13.223/32"]
  source       = "../../modules/access_controller"
  sec_group_id = module.api.deployment_controller_sec_group_id
}

resource "null_resource" "authorized_keys_file" {
  depends_on = [module.api]

  provisioner "local-exec" {
    command = "scp -o StrictHostKeyChecking=no -i ~/.ssh/${var.ssh_key_name}.pem ./authorized_keys ${var.linux_user}@${module.api.deployment_controller_public_ip}:/home/${var.linux_user}/.ssh"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${module.api.deployment_controller_public_ip} 'chmod 600 ~/.ssh/authorized_keys'"
  }
}