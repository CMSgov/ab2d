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

data "terraform_remote_state" "core" {
  backend = "s3"
  config  = {
    region         = var.region
    bucket         = "${var.env}-tfstate"
    key            = "${var.env}/terraform/core/terraform.tfstate"
  }
}

module "data" {
  source                     = "../../../modules/terraservices_pattern/data"
  aws_account_number         = var.aws_account_number
  controller_sg_id           = var.controller_sg_id
  cpm_backup_db              = var.cpm_backup_db
  db_allocated_storage_size  = var.db_allocated_storage_size
  db_backup_retention_period = var.db_backup_retention_period
  db_backup_window           = var.db_backup_window
  db_copy_tags_to_snapshot   = var.db_copy_tags_to_snapshot
  db_identifier              = var.db_identifier
  db_instance_class          = var.db_instance_class
  db_iops                    = var.db_iops
  db_maintenance_window      = var.db_maintenance_window
  db_multi_az                = var.db_multi_az
  db_parameter_group_name    = var.db_parameter_group_name
  db_password                = var.db_password
  db_snapshot_id             = var.db_snapshot_id
  db_subnet_group_name       = var.db_subnet_group_name
  db_username                = var.db_username
  env                        = var.env
  jenkins_agent_sec_group_id = var.jenkins_agent_sec_group_id
  main_kms_key_arn           = data.terraform_remote_state.core.outputs.main_kms_key_arn
  parent_env                 = var.parent_env
  postgres_engine_version    = var.postgres_engine_version
  private_subnet_a_id        = data.terraform_remote_state.core.outputs.private_subnet_a_id
  private_subnet_b_id        = data.terraform_remote_state.core.outputs.private_subnet_b_id
  region                     = var.region
  vpc_id                     = data.terraform_remote_state.core.outputs.vpc_id
}
