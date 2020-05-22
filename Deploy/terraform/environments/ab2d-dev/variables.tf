variable "mgmt_aws_account_number" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "aws_account_number" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "env" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "ab2d_spe_developer_policies" {
  type    = list(string)
  default = []
}

variable "vpc_id" {
  default = ""
  description = "Please pass this on command line and not as a value here"
}

## EC2 specific variables ########################################################################

variable "private_subnet_ids" {
  type        = list(string)
  default     = []
  description = "App instances and DB go here"
}

variable "deployment_controller_subnet_ids" {
  type        = list(string)
  default     = []
  description = "Deployment controllers go here"
}

variable "ami_id" {
  default     = ""
  description = "This is meant to be a different value on every new deployment"
}

variable "ec2_instance_type" {
  default = ""
}

variable "ec2_instance_type_api" {
  default = ""
}

variable "ec2_instance_type_worker" {
  default = ""
}

variable "linux_user" {
  default = ""
}

variable "ssh_key_name" {
  default = "ab2d-dev"
}

variable "max_ec2_instance_count" {
  default = "5"
}

variable "min_ec2_instance_count" {
  default = "5"
}

variable "desired_ec2_instance_count" {
  default = "5"
}

variable "elb_healtch_check_type" {
  default     = "EC2"
  description = "Values can be EC2 or ELB"
}

variable "autoscale_group_wait" {
  default     = "0"
  description = "Number of instances in service to wait for before activating autoscaling group"
}

# variable "elb_healthcheck_url" {
#   default = "HTTP:8080/"
# }

variable "ec2_iam_profile" {
  default = "Ab2dInstanceProfile"
}

variable "ec2_desired_instance_count_api" {
  default = ""
}

variable "ec2_minimum_instance_count_api" {
  default = ""
}

variable "ec2_maximum_instance_count_api" {
  default = ""
}

variable "ec2_desired_instance_count_worker" {
  default = ""
}

variable "ec2_minimum_instance_count_worker" {
  default = ""
}

variable "ec2_maximum_instance_count_worker" {
  default = ""
}

variable "gold_image_name" {
  default = "rhel7-gi-2020-04-22T23-29-10Z"
}

## RDS specific variables ########################################################################

variable "db_allocated_storage_size" {
  default = "500"
}

variable "postgres_engine_version" {
  default = "11.5"
}

variable "db_instance_class" {
  default = "db.m4.2xlarge"
}

variable "db_snapshot_id" {
  default = ""
}

variable "db_skip_final_snapshot" {
  default = "true"
}

variable "db_subnet_group_name" {
  default = "ab2d-rds-subnet-group"
}

variable "db_parameter_group_name" {
  default = "ab2d-rds-parameter-group"
}

variable "db_backup_retention_period" {
  default = "7"
}

variable "db_backup_window" {
  default = "03:15-03:45"
}

variable "db_copy_tags_to_snapshot" {
  default = "false"
}

variable "db_iops" {
  default = 5000
}

variable "db_maintenance_window" {
  default = "tue:10:26-tue:10:56"
}

variable "db_identifier" {
  default = "ab2d"
}

variable "db_multi_az" {
  default = "false"
}

variable "db_host" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_port" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_username" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_password" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_name" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_host_secret_arn" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_port_secret_arn" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_user_secret_arn" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_password_secret_arn" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_name_secret_arn" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "cpm_backup" {
  default = "Daily"
}

## S3 specific variables #########################################################################

variable "file_bucket_name" {
  default = "ab2d-dev"
}

variable "logging_bucket_name" {
  default = "ab2d-dev-cloudtrail"
}

## ECS specific variables #########################################################################

variable "current_task_definition_arn" {
  default     = ""
  description = "Please pass this on command line as part of deployment process"
}

variable "ecs_container_definition_new_memory_api" {
  default = ""
}

variable "ecs_task_definition_cpu_api" {
  default = ""
}

variable "ecs_task_definition_memory_api" {
  default = ""
}

variable "ecs_container_definition_new_memory_worker" {
  default = ""
}

variable "ecs_task_definition_cpu_worker" {
  default = ""
}

variable "ecs_task_definition_memory_worker" {
  default = ""
}

## SNS specific variables #########################################################################

variable "alert_email_address" {
  default = "lonnie.hanekamp@semanticbits.com"
}

variable "victorops_url_endpoint" {
  default = ""
}

variable "deployer_ip_address" {
  default = ""
  description = "Programmatically determined and passed in at the command line"
}

variable "ecr_repo_aws_account" {
  default = ""
  description = "Programmatically determined and passed in at the command line"
}

variable "image_version" {
  default = ""
  description = "Programmatically determined and passed in at the command line"
}

## BFD specific variables #########################################################################

variable "bfd_url" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "bfd_keystore_location" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "bfd_keystore_password" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "hicn_hash_pepper" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "hicn_hash_iter" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "bfd_keystore_file_name" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "new_relic_app_name" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "new_relic_license_key" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

## ALB specific variables #########################################################################

variable "ecs_task_definition_host_port" {
  type        = number
  default     = 443
  description = "Please pass this on command line and not as a value here"
}

variable "host_port" {
  type        = number
  default     = 443
  description = "Please pass this on command line and not as a value here"
}

variable "alb_listener_protocol" {
  type        = string
  default     = "HTTPS"
  description = "Please pass this on command line and not as a value here"
}

variable "alb_listener_certificate_arn" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "alb_internal" {
  type        = bool
  default     = true
  description = "Please pass this on command line and not as a value here"
}

variable "alb_security_group_ip_range" {
  type        = string
  default     = "0.0.0.0/0"
  description = "Please pass this on command line and not as a value here"
}

variable "vpn_private_ip_address_cidr_range" {
  default = ""
  description = "Please pass this on command line and not as a value here"
}

#
# Kinesis Firehose specific variables
#

variable "kinesis_firehose_bucket" {
  default = "bfd-insights-ab2d-577373831711"
}

variable "kinesis_firehose_delivery_streams" {
  default = [
    "api_request_event",
    "api_response_event",
    "beneficiary_search_event",
    "contract_bene_search_event",
    "error_event",
    "file_event",
    "job_status_change_event",
    "reload_event"
  ]
}

variable "kinesis_firehose_kms_key_arn" {
  default = "arn:aws:kms:us-east-1:577373831711:key/97973f21-cdc5-421e-83a8-8545b007999f"
}

variable "kinesis_firehose_role" {
  default = "Ab2dBfdInsightsRole"
}

#
# Worker
#

variable "claims_skip_billable_period_check" {
  default = "true"
}

#
# Jenkins Agent
#

variable "jenkins_agent_sec_group_id" {
  default = "653916833532/sg-0e370f9dcfe051ed0"
}
