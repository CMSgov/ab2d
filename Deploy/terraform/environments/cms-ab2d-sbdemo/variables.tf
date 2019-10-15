variable "aws_profile" {
  default = "sbdemo"
}

variable "env" {
  default = "sbdemo"
}

variable "vpc_id" {
  default = "vpc-00dcfaadb3fe8e3a2"
}

## EC2 specific variables ########################################################################

variable "private_subnet_ids" {
  type        = list(string)
  default     = ["subnet-09fe6a03c783e0e8b", "subnet-0a06d6c59dead565e", "subnet-0f705fee369c49184"]
  description = "App instances and DB go here"
}

variable "deployment_controller_subnet_ids" {
  type        = list(string)
  default     = ["subnet-077269e0fb659e953", "subnet-0f36ecc59af6ee4f4", "subnet-09aca9941679c01a0"]
  description = "Deployment controllers go here"
}

variable "ami_id" {
  default     = ""
  description = "This is meant to be a different value on every new deployment"
}

variable "ec2_instance_type" {
  default = "c4.2xlarge"
}

variable "linux_user" {
  default = "centos"
}

variable "ssh_key_name" {
  default = "ab2d-sbdemo"
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

variable "elb_healthcheck_url" {
  default = "HTTP:3000/"
}

variable "ec2_iam_profile" {
  default = "Ab2dInstanceProfile"
}

variable "ec2_desired_instance_count" {
  default = "2"
}

variable "ec2_minimum_instance_count" {
  default = "2"
}

variable "ec2_maximum_instance_count" {
  default = "2"
}

variable "gold_image_name" {
  default = "EAST-RH 7-6 Gold Image V.1.09 (HVM) 06-26-19"
}

# LSH SKIP FOR NOW BEGIN
# variable "enterprise-tools-sec-group-id" {
#   default = "sg-0566ad330966d8ba7"
# }
# LSH SKIP FOR NOW END

# LSH SKIP FOR NOW BEGIN
# variable "vpn-private-sec-group-id" {
#   default = "sg-07fbbd710a8b15851"
# }
# LSH SKIP FOR NOW END

## RDS specific variables ########################################################################

variable "db_allocated_storage_size" {
  default = "500"
}

variable "postgres_engine_version" {
  default = "10.6"
}

variable "db_instance_class" {
  default = "db.r4.4xlarge"
}

variable "db_snapshot_id" {
  default = ""
}

variable "db_skip_final_snapshot" {
  default = "true"
}

variable "db_subnet_group_name" {
  default = "cms-ab2d-dev-rdssubnetgroup"
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
  default = "cms-ab2d-dev"
}

variable "db_multi_az" {
  default = "false"
}

variable "db_username" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "db_password" {
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

## S3 specific variables #########################################################################

variable "file_bucket_name" {
  default = "cms-ab2d-dev"
}

variable "logging_bucket_name" {
  default = "cms-ab2d-cloudtrail"
}

variable "nlb_logging_bucket_name" {
  default = "cms-ab2d-cloudtrail-nlb"
}

variable "s3_username_whitelist" {
  default = ["lonnie.hanekamp@semanticbits.com"]
}

## ECS specific variables #########################################################################

variable "current_task_definition_arn" {
  default     = ""
  description = "Please pass this on command line as part of deployment process"
}

## SNS specific variables #########################################################################

variable "alert_email_address" {
  default = "lonnie.hanekamp@semanticbits.com"
}

variable "victorops_url_endpoint" {
  default = ""
}
