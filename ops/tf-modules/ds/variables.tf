#FIXME: should be using AWS SecretsManager for uname/password management from top-to-bottom
variable "username" {}
variable "password" {}
variable "platform" {}
variable "snapshot" {
  default = null
}

variable "kms_key_override" {
  default = null
}

variable "iops" {
  default = null
}

variable "storage_type" {
  default = null
}

variable "instance_class" {
}

variable "allocated_storage" {
}

variable "vpc_security_group_ids" {
  default = []
}

variable "multi_az" {
  default = false
}

variable "maintenance_window" {}
variable "backup_window" {}

variable "monitoring_interval" {
  default = 0
}

variable "performance_insights_enabled" {
  default = false
}

variable  "performance_insights_retention_period" {
  default = 0
}

variable "deletion_protection" {
  default = true
}
