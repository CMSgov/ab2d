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

//data "terraform_remote_state" "core" {
//  backend = "s3"
//  config  = {
//    region         = var.region
//    bucket         = "${var.env}-tfstate"
//    key            = "${var.env}/terraform/core/terraform.tfstate"
//  }
//}
//
//data "terraform_remote_state" "data" {
//  backend = "s3"
//  config = {
//    region = var.region
//    bucket = "${var.env}-tfstate"
//    key    = "${var.env}/terraform/data/terraform.tfstate"
//  }
//}

