# Required input vars
variable "env" {}
variable "vpc_id" {}
variable "bucket_name" {}
variable "encryption_key_arn" {}
variable "logging_bucket_name" {}
variable "username_list" {type = "list"}
