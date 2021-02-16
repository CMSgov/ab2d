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

data "terraform_remote_state" "data" {
  backend = "s3"
  config = {
    region = var.region
    bucket = "${var.env}-tfstate"
    key    = "${var.env}/terraform/data/terraform.tfstate"
  }
}

module "worker" {
  source                            = "../../../modules/terraservices_pattern/worker"
  alpha                             = data.terraform_remote_state.core.outputs.private_subnet_a_id
  ami_id                            = var.ami_id
  autoscale_group_wait              = "0" #Change this later for 0 downtime deployment
  aws_account_number                = var.aws_account_number
  beta                              = data.terraform_remote_state.core.outputs.private_subnet_b_id
  bfd_keystore_file_name            = var.bfd_keystore_file_name
  bfd_keystore_location             = var.bfd_keystore_location
  bfd_keystore_password             = var.bfd_keystore_password
  bfd_url                           = var.bfd_url
  claims_skip_billable_period_check = var.claims_skip_billable_period_check
  cpm_backup_worker                 = var.cpm_backup_worker
  db_host                           = var.db_host
  db_port                           = var.db_port
  db_name                           = var.db_name
  db_username                       = var.db_username
  db_password                       = var.db_password
  db_sec_group_id                   = data.terraform_remote_state.data.outputs.database_security_group_id
  ec2_instance_type_worker          = var.ec2_instance_type_worker
  ecr_repo_aws_account              = var.ecr_repo_aws_account
  ecs_container_def_memory          = var.ecs_container_def_memory
  ecs_task_def_cpu                  = var.ecs_task_def_cpu
  ecs_task_def_memory               = var.ecs_task_def_memory
  efs_dns_name                      = data.terraform_remote_state.core.outputs.efs_dns_name
  efs_id                            = data.terraform_remote_state.core.outputs.efs_id
  efs_security_group_id             = data.terraform_remote_state.core.outputs.efs_sg_id
  env                               = var.env
  execution_env                     = "ab2d-east-prod-test"
  gold_disk_name                    = var.gold_image_name
  hicn_hash_iter                    = var.hicn_hash_iter
  hicn_hash_pepper                  = var.hicn_hash_pepper
  iam_instance_profile              = var.iam_instance_profile
  iam_instance_role                 = var.iam_instance_role
  image_version                     = var.image_version
  new_relic_app_name                = var.new_relic_app_name
  new_relic_license_key             = var.new_relic_license_key
  node_subnet_ids                   = [data.terraform_remote_state.core.outputs.private_subnet_a_id, data.terraform_remote_state.core.outputs.private_subnet_b_id]
  override_task_definition_arn      = var.override_task_definition_arn
  percent_capacity_increase         = var.percent_capacity_increase
  region                            = var.region
  slack_alert_webhooks              = var.slack_alert_webhooks
  slack_trace_webhooks              = var.slack_trace_webhooks
  ssh_key_name                      = var.env
  ssh_username                      = var.ssh_username
  vpc_id                            = data.terraform_remote_state.core.outputs.vpc_id
  vpn_private_ip_address_cidr_range = var.vpn_private_ip_address_cidr_range
  worker_desired_instances          = var.worker_desired_instances
  worker_min_instances              = var.worker_min_instances
  worker_max_instances              = var.worker_max_instances
}
