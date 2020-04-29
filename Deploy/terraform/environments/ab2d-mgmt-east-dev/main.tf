provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
}

# Had to pass "-backend-config" parameters to "terraform init" since "Variables
# may not be used here"
terraform {
  backend "s3" {
  }
}

module "management_account" {
  source                  = "../../modules/management_account"
  mgmt_aws_account_number = var.mgmt_aws_account_number
  ab2d_mgmt_role_arn      = "arn:aws:iam::${var.aws_account_number}:role/Ab2dMgmtRole"
}