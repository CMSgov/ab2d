#
# Target environment variables
#

variable "env" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "aws_account_number" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "parent_env" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}



# #
# # BFD specific variables
# #

# variable "bfd_url" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "bfd_keystore_location" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "bfd_keystore_password" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "hicn_hash_pepper" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "hicn_hash_iter" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "bfd_keystore_file_name" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# #
# # EC2 variables
# #

# variable "ami_id" {
#   default     = ""
#   description = "This is meant to be a different value on every new deployment"
# }

# variable "ec2_instance_type_worker" {
#   default = ""
# }

# variable "linux_user" {
#   default = ""
# }

# variable "ssh_key_name" {
#   default = "ab2d-east-prod-test"
# }

# variable "ec2_iam_profile" {
#   default = "Ab2dProdTestInstanceV2Profile"
# }

# variable "gold_image_name" {
#   default = ""
# }

# #
# # ECR specific variables
# #

# variable "ecr_repo_aws_account" {
#   default = ""
#   description = "Programmatically determined and passed in at the command line"
# }

# variable "image_version" {
#   default = ""
#   description = "Programmatically determined and passed in at the command line"
# }

# #
# # ECS specific variables
# #

# variable "current_task_definition_arn" {
#   default     = ""
#   description = "Please pass this on command line as part of deployment process"
# }

# variable "ecs_container_definition_new_memory_api" {
#   default = ""
# }

# variable "ecs_task_definition_cpu_api" {
#   default = ""
# }

# variable "ecs_task_definition_memory_api" {
#   default = ""
# }

# variable "ecs_container_definition_new_memory_worker" {
#   default = ""
# }

# variable "ecs_task_definition_cpu_worker" {
#   default = ""
# }

# variable "ecs_task_definition_memory_worker" {
#   default = ""
# }

# #
# # EFS specific variables
# #

# variable "stunnel_latest_version" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# #
# # Management account variables
# #

# variable "mgmt_aws_account_number" {
#   type        = string
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "jenkins_agent_sec_group_id" {
#   type        = string
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# #
# # New relic specific variables
# #

# variable "new_relic_app_name" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "new_relic_license_key" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# #
# # Okta specific variables
# #

# variable "ab2d_okta_jwt_issuer" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# #
# # RDS specific variables
# #

# variable "db_allocated_storage_size" {
#   default = "500"
# }

# variable "postgres_engine_version" {
#   default = "11.5"
# }

# variable "db_instance_class" {
#   default = "db.m4.2xlarge"
# }

# variable "db_snapshot_id" {
#   default = ""
# }

# variable "db_skip_final_snapshot" {
#   default = "true"
# }

# variable "db_subnet_group_name" {
#   default = "ab2d-rds-subnet-group"
# }

# variable "db_parameter_group_name" {
#   default = "ab2d-rds-parameter-group"
# }

# variable "db_backup_retention_period" {
#   default = "7"
# }

# variable "db_backup_window" {
#   default = "03:15-03:45"
# }

# variable "db_copy_tags_to_snapshot" {
#   default = "false"
# }

# variable "db_iops" {
#   default = 5000
# }

# variable "db_maintenance_window" {
#   default = "tue:10:26-tue:10:56"
# }

# variable "db_identifier" {
#   default = "ab2d"
# }

# variable "db_multi_az" {
#   default = "true"
# }

# variable "db_host" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_port" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_username" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_password" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_name" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_host_secret_arn" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_port_secret_arn" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_user_secret_arn" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_password_secret_arn" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "db_name_secret_arn" {
#   default     = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "cpm_backup_api" {
#   default = "4HR Daily Weekly Monthly"
# }

# variable "cpm_backup_controller" {
#   default = "no-backup"
# }

# variable "cpm_backup_db" {
#   default = "4HR Daily Weekly Monthly"
# }

# variable "cpm_backup_worker" {
#   default = "4HR Daily Weekly Monthly"
# }

# #
# # S3 specific variables
# #

# variable "file_bucket_name" {
#   default = "ab2d-east-prod-test"
# }

# variable "logging_bucket_name" {
#   default = "ab2d-east-prod-test-cloudtrail"
# }

# #
# # SNS specific variables
# #

# variable "alert_email_address" {
#   default = "lonnie.hanekamp@semanticbits.com"
# }

# variable "victorops_url_endpoint" {
#   default = ""
# }

# #
# # VPC variables
# #

# variable "vpc_id" {
#   default = ""
#   description = "Please pass this on command line and not as a value here"
# }

# variable "private_subnet_ids" {
#   type        = list(string)
#   default     = []
#   description = "App instances and DB go here"
# }

# #
# # Worker specific environment variables
# #

# variable "claims_skip_billable_period_check" {
#   default = "false"
# }
