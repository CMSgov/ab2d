variable "username" {
  deprecated  = "This will no longer be supported once APIs adopt AWS Secrets Manager manged credentials."
  description = "The database's primary/master credentials username"
  type        = string
}

variable "password" {
  deprecated  = "This will no longer be supported once APIs adopt AWS Secrets Manager manged credentials."
  description = "The database's primary/master credentials password"
  type        = string
}

variable "platform" {
  description = "Object that describes standardized platform values."
  type        = any
}

variable "snapshot" {
  default     = null
  deprecated  = "This will no longer be supported once APIs have migrated to aurora. Use `aurora_snapshot` for aurora cluster data stores"
  description = "For use in restoring a snapshot to a traditional RDS DB Instance."
  type        = string
}

variable "kms_key_override" {
  default     = null
  description = "Override to the platform-managed KMS key"
  type        = string
}

variable "iops" {
  default    = null
  deprecated = "This will no longer be supported once APIs have migrated to aurora."
  type       = number
}

variable "storage_type" {
  default    = null
  deprecated = "This will no longer be supported once APIs have migrated to aurora."
  type       = string
}

variable "instance_class" {
  deprecated = "This will no longer be supported once APIs have migrated to aurora. Use `aurora_instance_class` for aurora cluster data stores."
  type       = string
}

variable "aurora_instance_class" {
  description = "Aurora cluster instance class"
  type        = string
}

variable "allocated_storage" {
  deprecated = "This will no longer be supported once APIs have migrated to aurora."
  type       = number
}

variable "vpc_security_group_ids" {
  default     = []
  description = "Additional security group ids for attachment to the data base security group."
  type        = list(string)
}

variable "multi_az" {
  default    = false
  deprecated = "This will no longer be supported once APIs have migrated to aurora."
  type       = bool
}

variable "maintenance_window" {
  description = "Weekly time range during which system maintenance can occur in UTC, e.g. `wed:04:00-wed:04:30`"
  type        = string
}

variable "backup_window" {
  description = "Daily time range during which automated backups are created if automated backups are enabled in UTC, e.g. `04:00-09:00`"
  type        = string
}

variable "monitoring_interval" {
  default     = 15
  description = "[monitoring_interval](https://registry.terraform.io/providers/hashicorp/aws/5.100.0/docs/resources/rds_cluster#monitoring_interval-1). Interval, in seconds, in seconds, between points when Enhanced Monitoring metrics are collected for the DB cluster."
  type        = number
}

variable "deletion_protection" {
  default     = true
  description = "If the DB cluster should have deletion protection enabled."
  type        = bool
}

variable "aurora_snapshot" {
  default     = null
  description = "Specifies whether or not to create this cluster from a snapshot, using snapshot name or ARN."
  type        = string
}

variable "monitoring_role_arn" {
  default     = null
  description = "ARN for the IAM role that permits RDS to send enhanced monitoring metrics to CloudWatch Logs."
  type        = string
}

variable "aurora_cluster_parameters" {
  default     = []
  description = "A list of objects containing the values for apply_method, name, and value that corresponds to the cluster-level prameters."
  type = list(object({
    apply_method = string
    name         = string
    value        = any
  }))
}

variable "aurora_cluster_instance_parameters" {
  default     = []
  description = "A list of objects containing the values for apply_method, name, and value that corresponds to the instance-level prameters."
  type = list(object({
    apply_method = string
    name         = string
    value        = any
  }))
}

variable "create_aurora_cluster" {
  default     = false
  deprecated  = "This will no longer be supported nor necessary once APIs have migrated to aurora."
  description = "When true, an aurora cluster will be created."
  type        = bool
}

variable "create_rds_db_instance" {
  default    = false
  deprecated = "This will no longer be supported once APIs have migrated to aurora."
  type       = bool
}

variable "engine_version" {
  default     = "16.8"
  description = "Selected engine version for either RDS DB Instance or RDS Aurora DB Cluster."
  type        = string
}

variable "backup_retention_period" {
  default     = 1
  description = "Days to retain backups for."
  type        = number
}

variable "aurora_storage_type" {
  default     = ""
  description = "Aurora cluster [storage_type](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/rds_cluster#storage_type-1)"
  type        = string
  validation {
    condition     = contains(["aurora-iopt1", ""], var.aurora_storage_type)
    error_message = "Aurora storage type only accepts 'aurora-iopt1' or an empty string ''."
  }
}

variable "cluster_identifier" {
  default     = null
  description = "Override for the aurora cluster identifier"
  type        = string
}
