terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=267771f3414c92e2f3090616587550e26bc41a47"
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
  source = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/sops?ref=267771f3414c92e2f3090616587550e26bc41a47"

  platform = module.platform
}


output "edit" {
  value = module.sops.sopsw
}
