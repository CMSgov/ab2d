#
# Target environment variables
#

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

variable "env_pascal_case" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "parent_env" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}

variable "region" {
  type        = string
  default     = ""
  description = "Please pass this on command line and not as a value here"
}
