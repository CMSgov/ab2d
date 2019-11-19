provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
  profile = var.aws_profile
}

terraform {
  backend "s3" {
    bucket         = "ab2d-automation"
    key            = "ab2d-sbdemo-dev/terraform/terraform.tfstate"
    region         = "us-east-1"
    encrypt = true
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
  encryption_key_arn  = "${data.aws_kms_key.ab2d_kms.arn}"
}

# LSH SKIP FOR NOW BEGIN
# vpn-private-sec-group-id      = var.vpn-private-sec-group-id
# enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
# LSH SKIP FOR NOW END
module "api" {
  source                        = "../../modules/api"
  env                           = var.env
  vpc_id                        = var.vpc_id
  db_sec_group_id               = "${data.aws_security_group.ab2d_database_sg.id}"
  controller_sec_group_id       = "${data.aws_security_group.ab2d_deployment_controller_sg.id}"
  controller_subnet_ids         = var.deployment_controller_subnet_ids
  ami_id                        = var.ami_id
  instance_type                 = var.ec2_instance_type
  linux_user                    = var.linux_user
  ssh_key_name                  = var.ssh_key_name
  node_subnet_ids               = var.private_subnet_ids
  logging_bucket                = var.logging_bucket_name
  healthcheck_url               = var.elb_healthcheck_url
  iam_instance_profile          = var.ec2_iam_profile
  docker_repository_url         = "${var.aws_account_number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_${var.env}_api:latest"
  iam_role_arn                  = "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole"
  desired_instances             = var.ec2_desired_instance_count
  min_instances                 = var.ec2_minimum_instance_count
  max_instances                 = var.ec2_maximum_instance_count
  autoscale_group_wait          = "0" #Change this later for 0 downtime deployment
  gold_disk_name                = var.gold_image_name
  override_task_definition_arn  = var.current_task_definition_arn
  aws_account_number            = var.aws_account_number
  db_host                       = "${data.aws_db_instance.ab2d.endpoint}"
  db_username                   = var.db_username
  db_password                   = var.db_password
  db_name                       = var.db_name
}

# LSH SKIP FOR NOW BEGIN
# vpn-private-sec-group-id      = var.vpn-private-sec-group-id
# enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
# LSH SKIP FOR NOW ENS
module "worker" {
  source                        = "../../modules/worker"
  env                           = var.env
  vpc_id                        = var.vpc_id
  db_sec_group_id               = "${data.aws_security_group.ab2d_database_sg.id}"
  controller_subnet_ids         = var.deployment_controller_subnet_ids
  ami_id                        = var.ami_id
  instance_type                 = var.ec2_instance_type
  linux_user                    = var.linux_user
  ssh_key_name                  = var.ssh_key_name
  node_subnet_ids               = var.private_subnet_ids
  iam_instance_profile          = var.ec2_iam_profile
  docker_repository_url         = "${var.aws_account_number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_${var.env}_worker:latest"
  desired_instances             = var.ec2_desired_instance_count
  min_instances                 = var.ec2_minimum_instance_count
  max_instances                 = var.ec2_maximum_instance_count
  autoscale_group_wait          = "0" #Change this later for 0 downtime deployment
  gold_disk_name                = var.gold_image_name
  override_task_definition_arn  = var.current_task_definition_arn
  app_sec_group_id              = module.api.application_security_group_id
  controller_sec_group_id       = "${data.aws_security_group.ab2d_deployment_controller_sg.id}"
  loadbalancer_subnet_ids       = var.deployment_controller_subnet_ids
  vpc_cidrs                     = ["10.124.1.0/24"]
  efs_id                        = module.efs.efs_id
  alpha                         = var.private_subnet_ids[0]
  beta                          = var.private_subnet_ids[1]
  ecs_cluster_id                = module.api.ecs_cluster_id
  aws_account_number            = var.aws_account_number
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