provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
  profile = var.aws_profile
}

# Had to pass "-backend-config" parameters to "terraform init" since "Variables
# may not be used here"
terraform {
  backend "s3" {
  }
}

module "kms" {
  source                  = "../../modules/kms"
  env                     = var.env
  aws_account_number      = var.aws_account_number
  ab2d_instance_role_name = "Ab2dInstanceRole"
}
