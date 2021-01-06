provider "aws" {
  region  = "us-east-1"
  version = "~> 2.21"
}

module "core" {
  source             = "../../../modules/terraservices_pattern/core"
  aws_account_number = var.aws_account_number
  env                = var.env
  env_pascal_case    = var.env_pascal_case
  parent_env         = var.parent_env
}

# Had to pass "-backend-config" parameters to "terraform init" since "Variables
# may not be used here"
terraform {
  backend "s3" {
  }
}
