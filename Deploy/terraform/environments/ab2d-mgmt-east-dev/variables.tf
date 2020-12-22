variable "env" {
  type        = string
  default     = "ab2d-mgmt-east-dev"
  description = "Please pass this on command line and not as a value here"
}

variable "mgmt_aws_account_number" {
  type    = string
  default = "653916833532"
}

variable "aws_account_number" {
  type        = string
  default     = "653916833532"
  description = "Please pass this on command line and not as a value here"
}

variable "mgmt_target_aws_account_mgmt_roles" {
  default = [
    "arn:aws:iam::653916833532:role/delegatedadmin/developer/Ab2dMgmtV2Role",
    "arn:aws:iam::349849222861:role/delegatedadmin/developer/Ab2dMgmtV2Role",
    "arn:aws:iam::777200079629:role/delegatedadmin/developer/Ab2dMgmtV2Role",
    "arn:aws:iam::330810004472:role/delegatedadmin/developer/Ab2dMgmtV2Role",
    "arn:aws:iam::595094747606:role/delegatedadmin/developer/Ab2dMgmtV2Role"
  ]
}

# Jenkins

variable "aws_profile" {
  default = "ab2d-mgmt-east-dev"
}

variable "vpc_id" {
  default = ""
}

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

variable "ec2_instance_tag" {
  default     = ""
  description = "This is meant to be a different value on every new deployment"
}

variable "federated_login_role_policies" {
  type    = list(string)
  default = []
  description = "Please pass this on command line and not as a value here"
}
