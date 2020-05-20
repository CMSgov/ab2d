variable "mgmt_aws_account_number" {
  type    = string
  default = "653916833532"
}

variable "ab2d_spe_developer_policies" {
  type    = list(string)
  default = []
}

variable "mgmt_target_aws_account_mgmt_roles" {
  default = [
    "arn:aws:iam::653916833532:role/Ab2dMgmtRole",
    "arn:aws:iam::349849222861:role/Ab2dMgmtRole",
    "arn:aws:iam::777200079629:role/Ab2dMgmtRole",
    "arn:aws:iam::330810004472:role/Ab2dMgmtRole",
    "arn:aws:iam::595094747606:role/Ab2dMgmtRole"
  ]
}
