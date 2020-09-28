variable "mgmt_aws_account_number" {
  type    = string
  default = "653916833532"
}

variable "aws_account_number" {
  type        = string
  default     = ""
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
