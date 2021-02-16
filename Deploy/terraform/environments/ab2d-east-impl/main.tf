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

module "iam" {
  source                      = "../../modules/iam"
  mgmt_aws_account_number     = var.mgmt_aws_account_number
  aws_account_number          = var.aws_account_number
  env                         = var.env
}

module "iam_bfd_insights" {
  source                      = "../../modules/iam_bfd_insights"
  mgmt_aws_account_number     = var.mgmt_aws_account_number
  aws_account_number          = var.aws_account_number
  env                         = var.env
  ab2d_bfd_insights_s3_bucket = var.ab2d_bfd_insights_s3_bucket
  ab2d_bfd_kms_arn            = var.ab2d_bfd_kms_arn
}

data "aws_iam_role" "ab2d_instance_role_name" {
  name = "Ab2dInstanceV2Role"
}

module "kms" {
  source                  = "../../modules/kms"
  aws_account_number      = var.aws_account_number
  env                     = var.env
  ab2d_instance_role_name = data.aws_iam_role.ab2d_instance_role_name.id
}

data "aws_kms_key" "ab2d_kms" {
  key_id = "alias/ab2d-kms"
}

module "db" {
  source                     = "../../modules/db"
  env                        = var.env
  allocated_storage_size     = var.db_allocated_storage_size
  engine_version             = var.postgres_engine_version
  instance_class             = var.db_instance_class
  snapshot_id                = var.db_snapshot_id
  subnet_group_name          = var.db_subnet_group_name
  parameter_group_name       = var.db_parameter_group_name
  backup_retention_period    = var.db_backup_retention_period
  backup_window              = var.db_backup_window
  copy_tags_to_snapshot      = var.db_copy_tags_to_snapshot
  iops                       = var.db_iops
  kms_key_id                 = data.aws_kms_key.ab2d_kms.arn
  maintenance_window         = var.db_maintenance_window
  vpc_id                     = var.vpc_id
  db_instance_subnet_ids     = var.private_subnet_ids
  identifier                 = var.db_identifier
  multi_az                   = var.db_multi_az
  username                   = var.db_username
  password                   = var.db_password
  skip_final_snapshot        = var.db_skip_final_snapshot
  cpm_backup_db              = var.cpm_backup_db
  jenkins_agent_sec_group_id = var.jenkins_agent_sec_group_id
}

# LSH SKIP FOR NOW BEGIN
# enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
# LSH SKIP FOR NOW END
module "controller" {
  source                            = "../../modules/controller"
  env                               = var.env
  vpc_id                            = var.vpc_id
  controller_subnet_ids             = var.deployment_controller_subnet_ids
  db_sec_group_id                   = module.db.aws_security_group_sg_database_id
  ami_id                            = var.ami_id
  instance_type                     = var.ec2_instance_type
  linux_user                        = var.linux_user
  ssh_key_name                      = var.ssh_key_name
  iam_instance_profile              = var.ec2_iam_profile
  gold_disk_name                    = var.gold_image_name
  deployer_ip_address               = var.deployer_ip_address
  vpn_private_ip_address_cidr_range = var.vpn_private_ip_address_cidr_range
  cpm_backup_controller             = var.cpm_backup_controller
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
    command = "scp -o StrictHostKeyChecking=no -i ~/.ssh/${var.ssh_key_name}.pem ./authorized_keys ${var.linux_user}@${module.controller.deployment_controller_private_ip}:/home/${var.linux_user}/.ssh"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${module.controller.deployment_controller_private_ip} 'chmod 600 ~/.ssh/authorized_keys'"
  }
}

data "aws_db_instance" "ab2d" {
  db_instance_identifier = "ab2d"
}

data "aws_instance" "ab2d_deployment_controller" {
  filter {
    name   = "tag:Name"
    values = ["ab2d-deployment-controller"]
  }
}

data "aws_security_group" "ab2d_database_sg" {
  filter {
    name   = "tag:Name"
    values = ["ab2d-database-sg"]
  }
}

data "aws_security_group" "ab2d_deployment_controller_sg" {
  filter {
    name   = "tag:Name"
    values = ["ab2d-deployment-controller-sg"]
  }
}

module "efs" {
  source              = "../../modules/efs"
  env                 = var.env
  vpc_id              = var.vpc_id
  encryption_key_arn  = data.aws_kms_key.ab2d_kms.arn
}

module "api" {
  source                            = "../../modules/api"
  env                               = var.env
  execution_env                     = "ab2d-east-impl"
  vpc_id                            = var.vpc_id
  db_sec_group_id                   = data.aws_security_group.ab2d_database_sg.id
  controller_sec_group_id           = data.aws_security_group.ab2d_deployment_controller_sg.id
  controller_subnet_ids             = var.deployment_controller_subnet_ids
  ami_id                            = var.ami_id
  instance_type                     = var.ec2_instance_type_api
  linux_user                        = var.linux_user
  ssh_key_name                      = var.ssh_key_name
  node_subnet_ids                   = var.private_subnet_ids
  efs_id                            = module.efs.efs_id
  efs_security_group_id             = module.efs.efs_security_group_id
  efs_dns_name                      = module.efs.efs_dns_name
  alpha                             = var.private_subnet_ids[0]
  beta                              = var.private_subnet_ids[1]
  logging_bucket                    = var.logging_bucket_name
  iam_instance_profile              = var.ec2_iam_profile
  iam_role_arn                      = "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/Ab2dInstanceV2Role"
  desired_instances                 = var.ec2_desired_instance_count_api
  min_instances                     = var.ec2_minimum_instance_count_api
  max_instances                     = var.ec2_maximum_instance_count_api
  autoscale_group_wait              = "0" #Change this later for 0 downtime deployment
  gold_disk_name                    = var.gold_image_name
  override_task_definition_arn      = var.current_task_definition_arn
  aws_account_number                = var.aws_account_number
  db_host                           = var.db_host
  db_port                           = var.db_port
  db_name                           = var.db_name
  db_username                       = var.db_username
  db_password                       = var.db_password
  db_host_secret_arn                = var.db_host_secret_arn
  db_port_secret_arn                = var.db_port_secret_arn
  db_user_secret_arn                = var.db_user_secret_arn
  db_password_secret_arn            = var.db_password_secret_arn
  db_name_secret_arn                = var.db_name_secret_arn
  deployer_ip_address               = var.deployer_ip_address
  ecr_repo_aws_account              = var.ecr_repo_aws_account
  image_version                     = var.image_version
  new_relic_app_name                = var.new_relic_app_name
  new_relic_license_key             = var.new_relic_license_key
  ecs_container_def_memory          = var.ecs_container_definition_new_memory_api
  ecs_task_def_cpu                  = var.ecs_task_definition_cpu_api
  ecs_task_def_memory               = var.ecs_task_definition_memory_api
  ecs_task_definition_host_port     = var.ecs_task_definition_host_port
  host_port                         = var.host_port
  alb_listener_protocol             = var.alb_listener_protocol
  alb_listener_certificate_arn      = var.alb_listener_certificate_arn
  alb_internal                      = var.alb_internal
  alb_security_group_ip_range       = var.alb_security_group_ip_range
  vpn_private_ip_address_cidr_range = var.vpn_private_ip_address_cidr_range
  ab2d_keystore_location            = var.ab2d_keystore_location
  ab2d_keystore_password            = var.ab2d_keystore_password
  ab2d_okta_jwt_issuer              = var.ab2d_okta_jwt_issuer
  ab2d_hpms_url                     = var.ab2d_hpms_url
  ab2d_hpms_api_params              = var.ab2d_hpms_api_params
  ab2d_hpms_auth_key_id             = var.ab2d_hpms_auth_key_id
  ab2d_hpms_auth_key_secret         = var.ab2d_hpms_auth_key_secret
  ab2d_slack_alert_webhooks         = var.ab2d_slack_alert_webhooks
  ab2d_slack_trace_webhooks         = var.ab2d_slack_trace_webhooks
  cpm_backup_api                    = var.cpm_backup_api
}

module "worker" {
  source                            = "../../modules/worker"
  env                               = var.env
  execution_env                     = "ab2d-east-impl"
  vpc_id                            = var.vpc_id
  db_sec_group_id                   = data.aws_security_group.ab2d_database_sg.id
  controller_subnet_ids             = var.deployment_controller_subnet_ids
  ami_id                            = var.ami_id
  instance_type                     = var.ec2_instance_type_worker
  linux_user                        = var.linux_user
  ssh_key_name                      = var.ssh_key_name
  node_subnet_ids                   = var.private_subnet_ids
  iam_instance_profile              = var.ec2_iam_profile
  desired_instances                 = var.ec2_desired_instance_count_worker
  min_instances                     = var.ec2_minimum_instance_count_worker
  max_instances                     = var.ec2_maximum_instance_count_worker
  autoscale_group_wait              = "0" #Change this later for 0 downtime deployment
  gold_disk_name                    = var.gold_image_name
  override_task_definition_arn      = var.current_task_definition_arn
  app_sec_group_id                  = module.api.application_security_group_id
  controller_sec_group_id           = data.aws_security_group.ab2d_deployment_controller_sg.id
  loadbalancer_subnet_ids           = var.deployment_controller_subnet_ids
  efs_id                            = module.efs.efs_id
  efs_security_group_id             = module.efs.efs_security_group_id
  efs_dns_name                      = module.efs.efs_dns_name
  ecs_cluster_id                    = module.api.ecs_cluster_id
  aws_account_number                = var.aws_account_number
  db_host                           = var.db_host
  db_port                           = var.db_port
  db_name                           = var.db_name
  db_username                       = var.db_username
  db_password                       = var.db_password
  db_host_secret_arn                = var.db_host_secret_arn
  db_port_secret_arn                = var.db_port_secret_arn
  db_user_secret_arn                = var.db_user_secret_arn
  db_password_secret_arn            = var.db_password_secret_arn
  db_name_secret_arn                = var.db_name_secret_arn
  ecr_repo_aws_account              = var.ecr_repo_aws_account
  image_version                     = var.image_version
  bfd_url                           = var.bfd_url
  bfd_keystore_location             = var.bfd_keystore_location
  bfd_keystore_password             = var.bfd_keystore_password
  hicn_hash_pepper                  = var.hicn_hash_pepper
  hicn_hash_iter                    = var.hicn_hash_iter
  bfd_keystore_file_name            = var.bfd_keystore_file_name
  new_relic_app_name                = var.new_relic_app_name
  new_relic_license_key             = var.new_relic_license_key
  ecs_container_def_memory          = var.ecs_container_definition_new_memory_worker
  ecs_task_def_cpu                  = var.ecs_task_definition_cpu_worker
  ecs_task_def_memory               = var.ecs_task_definition_memory_worker
  vpn_private_ip_address_cidr_range = var.vpn_private_ip_address_cidr_range
  claims_skip_billable_period_check = var.claims_skip_billable_period_check
  ab2d_opt_out_job_schedule         = var.ab2d_opt_out_job_schedule
  ab2d_s3_optout_bucket             = var.ab2d_s3_optout_bucket
  ab2d_slack_alert_webhooks         = var.ab2d_slack_alert_webhooks
  ab2d_slack_trace_webhooks         = var.ab2d_slack_trace_webhooks
  cpm_backup_worker                 = var.cpm_backup_worker
}

module "cloudwatch" {
  source                  = "../../modules/cloudwatch"
  env                     = var.env
  autoscaling_arn         = module.api.aws_autoscaling_policy_percent_capacity_arn
  # sns_arn                 = module.sns.aws_sns_topic_ab2d_alarms_arn
  sns_arn                 = "arn:aws:sns:us-east-1:${var.aws_account_number}:${var.env}-cloudwatch-alarms"
  autoscaling_name        = module.api.aws_autoscaling_group_name
  controller_server_id    = data.aws_instance.ab2d_deployment_controller.instance_id
  s3_bucket_name          = var.file_bucket_name
  db_name                 = var.db_identifier
  # target_group_arn_suffix = module.api.alb_target_group_arn_suffix
  target_group_arn_suffix = var.target_group_arn_suffix
  loadbalancer_arn_suffix = module.api.alb_arn_suffix
}

module "waf" {
  source  = "../../modules/waf"
  env     = var.env
  alb_arn = module.api.alb_arn
}

# Kinesis Firehose

module "kinesis_firehose" {
  source                            = "../../modules/kinesis_firehose"
  aws_account_number                = var.aws_account_number
  env                               = var.env
  kinesis_firehose_bucket           = var.kinesis_firehose_bucket
  kinesis_firehose_delivery_streams = var.kinesis_firehose_delivery_streams
  kinesis_firehose_kms_key_arn      = var.kinesis_firehose_kms_key_arn
  kinesis_firehose_role             = var.kinesis_firehose_role
}

# Management Target

module "management_target" {
  source                        = "../../modules/management_target"
  env                           = var.env
  mgmt_aws_account_number       = var.mgmt_aws_account_number
  aws_account_number            = var.aws_account_number
  federated_login_role_policies = var.federated_login_role_policies
}
