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

data "aws_db_instance" "ab2d" {
  db_instance_identifier = "ab2d"
}

data "aws_instance" "ab2d_deployment_controller" {
  filter {
    name   = "tag:Name"
    values = ["ab2d-deployment-controller"]
  }
}

data "aws_kms_key" "ab2d_kms" {
  key_id = "alias/ab2d-kms"
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
  encryption_key_arn  = "${data.aws_kms_key.ab2d_kms.arn}"
}

module "api" {
  source                        = "../../modules/api"
  env                           = var.env
  vpc_id                        = var.vpc_id
  db_sec_group_id               = "${data.aws_security_group.ab2d_database_sg.id}"
  controller_sec_group_id       = "${data.aws_security_group.ab2d_deployment_controller_sg.id}"
  controller_subnet_ids         = var.deployment_controller_subnet_ids
  ami_id                        = var.ami_id
  instance_type                 = var.ec2_instance_type_api
  linux_user                    = var.linux_user
  ssh_key_name                  = var.ssh_key_name
  node_subnet_ids               = var.private_subnet_ids
  efs_id                        = module.efs.efs_id
  efs_security_group_id         = module.efs.efs_security_group_id
  efs_dns_name                  = module.efs.efs_dns_name
  alpha                         = var.private_subnet_ids[0]
  beta                          = var.private_subnet_ids[1]
  logging_bucket                = var.logging_bucket_name
  # healthcheck_url               = var.elb_healthcheck_url
  iam_instance_profile          = var.ec2_iam_profile
  iam_role_arn                  = "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole"
  desired_instances             = var.ec2_desired_instance_count_api
  min_instances                 = var.ec2_minimum_instance_count_api
  max_instances                 = var.ec2_maximum_instance_count_api
  autoscale_group_wait          = "0" # 0 for quick deployment; 4 for 0 downtime deployment
  gold_disk_name                = var.gold_image_name
  override_task_definition_arn  = var.current_task_definition_arn
  aws_account_number            = var.aws_account_number
  db_host                       = var.db_host
  db_port                       = var.db_port
  db_name                       = var.db_name
  db_username                   = var.db_username
  db_password                   = var.db_password
  db_host_secret_arn            = var.db_host_secret_arn
  db_port_secret_arn            = var.db_port_secret_arn
  db_user_secret_arn            = var.db_user_secret_arn
  db_password_secret_arn        = var.db_password_secret_arn
  db_name_secret_arn            = var.db_name_secret_arn
  deployer_ip_address           = var.deployer_ip_address
  ecr_repo_aws_account          = var.ecr_repo_aws_account
  image_version                 = var.image_version
  new_relic_app_name            = var.new_relic_app_name
  new_relic_license_key         = var.new_relic_license_key
  ecs_container_def_memory      = var.ecs_container_definition_new_memory_api
  ecs_task_def_cpu              = var.ecs_task_definition_cpu_api
  ecs_task_def_memory           = var.ecs_task_definition_memory_api
  ecs_task_definition_host_port = var.ecs_task_definition_host_port
  host_port                     = var.host_port
  alb_listener_protocol         = var.alb_listener_protocol
  alb_listener_certificate_arn  = var.alb_listener_certificate_arn
  alb_internal                  = var.alb_internal
  alb_security_group_ip_range   = var.alb_security_group_ip_range
  vpn_private_ip_address_cidr_range = var.vpn_private_ip_address_cidr_range
}

module "worker" {
  source                          = "../../modules/worker"
  env                             = var.env
  vpc_id                          = var.vpc_id
  db_sec_group_id                 = "${data.aws_security_group.ab2d_database_sg.id}"
  controller_subnet_ids           = var.deployment_controller_subnet_ids
  ami_id                          = var.ami_id
  instance_type                   = var.ec2_instance_type_worker
  linux_user                      = var.linux_user
  ssh_key_name                    = var.ssh_key_name
  node_subnet_ids                 = var.private_subnet_ids
  iam_instance_profile            = var.ec2_iam_profile
  desired_instances               = var.ec2_desired_instance_count_worker
  min_instances                   = var.ec2_minimum_instance_count_worker
  max_instances                   = var.ec2_maximum_instance_count_worker
  autoscale_group_wait            = "0" #Change this later for 0 downtime deployment
  gold_disk_name                  = var.gold_image_name
  override_task_definition_arn    = var.current_task_definition_arn
  app_sec_group_id                = module.api.application_security_group_id
  controller_sec_group_id         = "${data.aws_security_group.ab2d_deployment_controller_sg.id}"
  loadbalancer_subnet_ids         = var.deployment_controller_subnet_ids
  efs_id                          = module.efs.efs_id
  efs_security_group_id           = module.efs.efs_security_group_id
  efs_dns_name                    = module.efs.efs_dns_name
  ecs_cluster_id                  = module.api.ecs_cluster_id
  aws_account_number              = var.aws_account_number
  db_host                         = var.db_host
  db_port                         = var.db_port
  db_name                         = var.db_name
  db_username                     = var.db_username
  db_password                     = var.db_password
  db_host_secret_arn              = var.db_host_secret_arn
  db_port_secret_arn              = var.db_port_secret_arn
  db_user_secret_arn              = var.db_user_secret_arn
  db_password_secret_arn          = var.db_password_secret_arn
  db_name_secret_arn              = var.db_name_secret_arn
  ecr_repo_aws_account            = var.ecr_repo_aws_account
  image_version                   = var.image_version
  bfd_url                         = var.bfd_url
  bfd_keystore_location           = var.bfd_keystore_location
  bfd_keystore_password           = var.bfd_keystore_password
  hicn_hash_pepper                = var.hicn_hash_pepper
  hicn_hash_iter                  = var.hicn_hash_iter
  bfd_keystore_file_name          = var.bfd_keystore_file_name
  new_relic_app_name              = var.new_relic_app_name
  new_relic_license_key           = var.new_relic_license_key
  ecs_container_def_memory        = var.ecs_container_definition_new_memory_worker
  ecs_task_def_cpu                = var.ecs_task_definition_cpu_worker
  ecs_task_def_memory             = var.ecs_task_definition_memory_worker
  vpn_private_ip_address_cidr_range = var.vpn_private_ip_address_cidr_range
}

module "cloudwatch" {
  source                  = "../../modules/cloudwatch"
  env                     = var.env
  autoscaling_arn         = module.api.aws_autoscaling_policy_percent_capacity_arn
  # sns_arn                 = module.sns.aws_sns_topic_AB2D-Alarms_arn
  autoscaling_name        = module.api.aws_autoscaling_group_name
  controller_server_id    = "${data.aws_instance.ab2d_deployment_controller.instance_id}"
  s3_bucket_name          = var.file_bucket_name
  db_name                 = var.db_identifier
  target_group_arn_suffix = module.api.alb_target_group_arn_suffix
  loadbalancer_arn_suffix = module.api.alb_arn_suffix
}

module "waf" {
  source  = "../../modules/waf"
  env     = var.env
  alb_arn = module.api.alb_arn
}
