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

module "iam" {
  source                      = "../../modules/iam"
  mgmt_aws_account_number     = var.mgmt_aws_account_number
  aws_account_number          = var.aws_account_number
  env                         = var.env
  bfd_opt_out_kms_arn         = var.bfd_opt_out_kms_arn
  ab2d_s3_optout_bucket       = var.ab2d_s3_optout_bucket
  ab2d_bfd_insights_s3_bucket = var.ab2d_bfd_insights_s3_bucket
  ab2d_bfd_kms_arn            = var.ab2d_bfd_kms_arn
}

data "aws_iam_role" "ab2d_instance_role_name" {
  name = "Ab2dInstanceV2Role"
}
