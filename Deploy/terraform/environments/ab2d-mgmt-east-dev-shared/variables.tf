variable "aws_account_number" {
  default = "653916833532"
}

variable "aws_profile" {
  default = "ab2d-mgmt-east-dev"
}

variable "env" {
  default = "ab2d-mgmt-east-dev-shared"
}

variable "vpc_id" {
  default = ""
}

## EC2 specific variables ########################################################################

variable "private_subnet_ids" {
  type        = list(string)
  default     = []
  description = "App instances and DB go here"
}

variable "public_subnet_ids" {
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

variable "linux_user" {
  default = ""
}

variable "ssh_key_name" {
  default = "ab2d-mgmt-east-dev"
}

variable "ec2_iam_profile" {
  default = "Ab2dInstanceV2Profile"
}

variable "vpn_private_sec_group_id" {
  default = "sg-0ad046da6829642d4"
}
