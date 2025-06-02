terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = local.app
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/10-config"
  service     = local.service
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "config"
}

module "sops" {
  source = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/sops?ref=PLT-1099"

  platform = module.platform
}


output "edit" {
  value = module.sops.sopsw
}
