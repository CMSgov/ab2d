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
    bucket         = "sb-terraform-ab2d"
    key            = "cms-ab2d-dev/terraform/terraform.tfstate"
    region         = "us-east-1"
    encrypt = true
  }
}

module "kms" {
  source = "../../modules/kms"
  env    = var.env
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

module "app" {
  source                        = "../../modules/app"
  env                           = var.env
  vpc_id                        = var.vpc_id
  db_sec_group_id               = module.db.aws_security_group_sg_database_id
  controller_subnet_ids         = var.deployment_controller_subnet_ids
  ami_id                        = var.ami_id
  instance_type                 = var.ec2_instance_type
  ssh_key_name                  = var.ssh_key_name
  node_subnet_ids               = var.private_subnet_ids
  logging_bucket                = var.logging_bucket_name
  healthcheck_url               = var.elb_healthcheck_url
  iam_instance_profile          = var.ec2_iam_profile
  docker_repository_url         = "626512334475.dkr.ecr.us-east-1.amazonaws.com/ab2d:ab2d-server-develop-5d7a5837b8687e02356870579d5ad0f160b757d3"
  iam_role_arn                  = "arn:aws:iam::626512334475:role/AB2D"
  desired_instances             = var.ec2_desired_instance_count
  min_instances                 = var.ec2_minimum_instance_count
  max_instances                 = var.ec2_maximum_instance_count
  autoscale_group_wait          = "0" #Change this later for 0 downtime deployment
  gold_disk_name                = var.gold_image_name
  override_task_definition_arn  = var.current_task_definition_arn
  vpn-private-sec-group-id      = var.vpn-private-sec-group-id
  enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
}
