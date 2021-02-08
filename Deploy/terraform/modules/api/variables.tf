variable "env" {}
variable "execution_env" { default = "local" }
variable "bfd_insights" { default = "false" }
variable "aws_account_number" {}
variable "vpc_id" {}
variable "db_sec_group_id" {}
variable "controller_sec_group_id" {}
variable "controller_subnet_ids" {type=list(string)}
variable "ami_id" {}
variable "instance_type" {}
variable "linux_user" {}
variable "ssh_key_name" {}
variable "node_subnet_ids" {type=list(string)}
variable "logging_bucket" {}
# variable "healthcheck_url" {}
variable "iam_instance_profile" {}
variable "iam_role_arn" {}

variable "container_port" {default=8443}
# variable "ecs_task_definition_host_port" {default=443}
# variable "host_port" {default=443}
variable "ecs_task_definition_host_port" {type = number}
variable "host_port" {type = number}

variable "alb_listener_protocol" {type = string}
variable "alb_listener_certificate_arn" {type = string}
variable "alb_internal" {type = bool}

variable "desired_instances" {}
variable "min_instances" {}
variable "max_instances" {}
variable "autoscale_group_wait" {}
variable "gold_disk_name" {}
variable "override_task_definition_arn" {default=""}
variable "ecs_container_def_memory" {}
variable "ecs_task_def_cpu" {}
variable "ecs_task_def_memory" {}

# LSH SKIP FOR NOW BEGIN
# variable "enterprise-tools-sec-group-id" {}
# variable "vpn-private-sec-group-id" {}
# LSH SKIP FOR NOW END

variable "percent_capacity_increase" {default="20"}

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

variable "deployer_ip_address" {}
variable "ecr_repo_aws_account" {}
variable "image_version" {}

variable "efs_id" {}
variable "efs_security_group_id" {}
variable "efs_dns_name" {}
variable "alpha" {}
variable "beta" {}

variable "new_relic_app_name" {}
variable "new_relic_license_key" {}

variable "alb_security_group_ip_range" {}
variable "vpn_private_ip_address_cidr_range" {}

variable "ab2d_keystore_location" {}
variable "ab2d_keystore_password" {}

variable "ab2d_okta_jwt_issuer" {}

variable "cpm_backup_api" {}

variable "ab2d_hpms_url" {}
variable "ab2d_hpms_api_params" {}
variable "ab2d_hpms_auth_key_id" {}
variable "ab2d_hpms_auth_key_secret" {}

variable "ab2d_slack_alert_webhooks" {}
variable "ab2d_slack_trace_webhooks" {}
