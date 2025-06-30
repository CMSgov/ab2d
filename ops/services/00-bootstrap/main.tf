module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = "ab2d"
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/00-bootstrap"
  service     = local.service
}

locals {
  service      = "bootstrap"
  default_tags = module.platform.default_tags
  env          = terraform.workspace

  ecr_container_repositories = toset([
    "ab2d-api",
    "ab2d-worker",
    "ab2d-properties",
    "ab2d-contracts",
    "ab2d-events"
  ])
}

module "sops" {
  source = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/sops?ref=PLT-1099"

  platform = module.platform
}

resource "aws_ecr_repository" "this" {
  for_each = local.ecr_container_repositories

  name                 = each.value
  image_tag_mutability = "MUTABLE"

  encryption_configuration {
    encryption_type = "KMS"
  }

  tags = {
    # strip ab2d- from container repository name to yield service
    service = replace(each.value, "ab2d-", "")
  }

  # ECR Repository-Level Scanning is Deprecated
  # Leave this `false` until it is fully removed from the API, provider
  image_scanning_configuration {
    scan_on_push = false
  }
}

