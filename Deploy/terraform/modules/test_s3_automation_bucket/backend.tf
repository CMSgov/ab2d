# Experimental currently unused module

terraform {
  backend "s3" {
    bucket         = "ab2d-east-prod-test-terraform-state"
    key            = "ab2d-east-prod-test/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "ab2d-east-prod-test-terraform-table"
    encrypt        = "1"
    kms_key_id     = "alias/ab2d-east-prod-test-terraform-state-kms"
  }
}
