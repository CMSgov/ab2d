variable "alpha" {}
variable "ami_id" {}
variable "autoscale_group_wait" {}
variable "aws_account_number" {}
variable "beta" {}
variable "bfd_insights" { default = "none" }
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
variable "execution_env" { default = "local" }
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
variable "slack_alert_webhooks" {}
variable "slack_trace_webhooks" {}
variable "ssh_key_name" {}
variable "ssh_username" {}
variable "vpc_id" {}
variable "vpn_private_ip_address_cidr_range" {}
variable "worker_desired_instances" {}
variable "worker_min_instances" {}
variable "worker_max_instances" {}