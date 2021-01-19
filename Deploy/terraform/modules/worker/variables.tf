variable "env" {}
variable "execution_env" {}
variable "aws_account_number" {}
variable "vpc_id" {}
variable "db_sec_group_id" {}
variable "container_port" {default=8080}
variable "host_port" {default=8080}
variable "controller_sec_group_id" {}
variable "app_sec_group_id" {}
variable "loadbalancer_subnet_ids" {type=list(string)}
variable "controller_subnet_ids" {type=list(string)}
variable "ami_id" {}
variable "gold_disk_name" {}
variable "instance_type" {}
variable "linux_user" {}
variable "iam_instance_profile" {}
variable "ssh_key_name" {}

# LSH SKIP FOR NOW BEGIN
# variable "enterprise-tools-sec-group-id" {}
# variable "vpn-private-sec-group-id" {}
# LSH SKIP FOR NOW END

variable "max_instances" {}
variable "min_instances" {}
variable "desired_instances" {}
variable "autoscale_group_wait" {}
variable "node_subnet_ids" {type=list(string)}
variable "percent_capacity_increase" {default="20"}
variable "override_task_definition_arn" {default=""}
variable "ecs_container_def_memory" {}
variable "ecs_task_def_cpu" {}
variable "ecs_task_def_memory" {}

# LSH BEGIN
# variable "docker_repository_url" {}
variable "ecs_cluster_id" {}
# LSH END

# LSH BEGIN 12/05/2019

variable "db_host" {}
variable "db_port" {}
variable "db_name" {}
variable "db_username" {}
variable "db_password" {}

variable "db_host_secret_arn" {}
variable "db_port_secret_arn" {}
variable "db_user_secret_arn" {}
variable "db_password_secret_arn" {}
variable "db_name_secret_arn" {}

# LSH END 12/05/2019

variable "ecr_repo_aws_account" {}
variable "image_version" {}

variable "efs_id" {}
variable "efs_security_group_id" {}
variable "efs_dns_name" {}

variable "bfd_url" {}
variable "bfd_keystore_location" {}
variable "bfd_keystore_password" {}

variable "hicn_hash_pepper" {}
variable "hicn_hash_iter" {}

# Used in userdata.tpl
variable "bfd_keystore_file_name" {}

variable "new_relic_app_name" {}
variable "new_relic_license_key" {}

variable "vpn_private_ip_address_cidr_range" {}

variable "claims_skip_billable_period_check" {}

variable "ab2d_opt_out_job_schedule" {}
variable "ab2d_s3_optout_bucket" {}

variable "cpm_backup_worker" {}
