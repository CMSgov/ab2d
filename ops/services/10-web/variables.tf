variable "alb_arn" {
  description = ""
}
variable "aws_account_number" {
  description = ""  
}
variable "autoscaling_arn" {
  description = ""
}
variable "autoscaling_name" {
  description = ""
}

variable "loadbalancer_arn_suffix" {
  description = ""
}
variable "sns_arn" {
 description = ""
}
variable "target_group_arn_suffix" {
  description = ""
}
variable "env_pascal_case" {
  description = ""
}

variable "legacy" {
  description = "Is this deployment in the greenfield environment (false)?"
  type        = bool
  default     = true
}

# Use because default tags are not possible
# Do not use to tag waf or cloudwatch alarm resources
variable "tags" {
  type = map
}
