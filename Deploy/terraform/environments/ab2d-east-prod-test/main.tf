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

module "test_iam" {
  source                      = "../../modules/test_iam"
  aws_account_number          = var.aws_account_number
  env                         = var.env
  parent_env                  = var.parent_env
}

data "aws_iam_role" "ab2d_instance_role_name" {
  name = "Ab2dProdTestInstanceRole"
}
