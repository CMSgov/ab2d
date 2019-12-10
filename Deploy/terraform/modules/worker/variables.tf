variable "env" {}
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
variable "vpc_cidrs" {type=list(string)}

# LSH BEGIN
variable "docker_repository_url" {}
variable "efs_id" {}
variable "alpha" {}
variable "beta" {}
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