variable "ami_id" {}
variable "autoscale_group_wait" {}
variable "aws_account_number" {}
variable "bfd_keystore_file_name" {} # Used in userdata.tpl
variable "bfd_keystore_location" {}
variable "bfd_keystore_password" {}
variable "bfd_url" {}
variable "claims_skip_billable_period_check" {}
variable "cpm_backup_worker" {}
variable "db_host" {}
variable "db_name" {}
variable "db_password" {}
variable "db_port" {}
variable "db_sec_group_id" {}
variable "db_username" {}
variable "ec2_instance_type_worker" {}
variable "ecr_repo_aws_account" {}
variable "ecs_container_def_memory" {}
variable "ecs_task_def_cpu" {}
variable "ecs_task_def_memory" {}
variable "efs_dns_name" {}
variable "efs_id" {}
variable "efs_security_group_id" {}
variable "env" {}
variable "execution_env" {}
variable "gold_disk_name" {}
variable "hicn_hash_iter" {}
variable "hicn_hash_pepper" {}
variable "iam_instance_profile" {}
variable "iam_instance_role" {}
variable "image_version" {}
variable "new_relic_app_name" {}
variable "new_relic_license_key" {}
variable "node_subnet_ids" {type=list(string)}
variable "override_task_definition_arn" {}
variable "percent_capacity_increase" {}
variable "region" {}
variable "ssh_key_name" {}
variable "ssh_username" {}
variable "stunnel_latest_version" {}
variable "vpc_id" {}
variable "vpn_private_ip_address_cidr_range" {}
variable "worker_desired_instances" {}
variable "worker_min_instances" {}
variable "worker_max_instances" {}





//variable "ab2d_opt_out_job_schedule" {}
//variable "ab2d_s3_optout_bucket" {}
//variable "app_sec_group_id" {}
//variable "container_port" {default=8080}
//variable "controller_sec_group_id" {}
//variable "controller_subnet_ids" {type=list(string)}
//variable "db_host_secret_arn" {}
//variable "db_port_secret_arn" {}
//variable "db_name_secret_arn" {}
//variable "db_user_secret_arn" {}
//variable "db_password_secret_arn" {}
//variable "ecs_cluster_id" {}
//variable "host_port" {default=8080}
//variable "loadbalancer_subnet_ids" {type=list(string)}
//variable "override_task_definition_arn" {default=""}
//variable "percent_capacity_increase" {default="20"}
