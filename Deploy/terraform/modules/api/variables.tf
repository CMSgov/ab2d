variable "env" {}
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
variable "healthcheck_url" {}
variable "iam_instance_profile" {}
variable "docker_repository_url" {}
variable "iam_role_arn" {}

# LSH BEGIN
# variable "container_port" {default=3000}
# variable "host_port" {default=80}
variable "container_port" {default=8080}
variable "ecs_task_definition_host_port" {default=80}
variable "host_port" {default=80}
# LSH END

variable "desired_instances" {}
variable "min_instances" {}
variable "max_instances" {}
variable "autoscale_group_wait" {}
variable "gold_disk_name" {}
variable "override_task_definition_arn" {default=""}

# LSH SKIP FOR NOW BEGIN
# variable "enterprise-tools-sec-group-id" {}
# variable "vpn-private-sec-group-id" {}
# LSH SKIP FOR NOW END

variable "percent_capacity_increase" {default="20"}

# LSH BEGIN 12/05/2019

# variable "db_host" {}
# variable "db_name" {}
# variable "db_username" {}
# variable "db_password" {}

variable "db_host_secret_arn" {}
variable "db_port_secret_arn" {}
variable "db_user_secret_arn" {}
variable "db_password_secret_arn" {}
variable "db_name_secret_arn" {}

# LSH END 12/05/2019

variable "deployer_ip_address" {}