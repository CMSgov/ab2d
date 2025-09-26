module "platform" {
  source    = "github.com/CMSgov/cdap//terraform/modules/platform?ref=ff2ef539fb06f2c98f0e3ce0c8f922bdacb96d66"
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
  source = "github.com/CMSgov/cdap//terraform/modules/sops?ref=ff2ef539fb06f2c98f0e3ce0c8f922bdacb96d66"

  platform = module.platform
}

resource "aws_kms_key" "this" {
  description             = "ab2d-ecr"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}

resource "aws_kms_alias" "this" {
  name          = "alias/ab2d-ecr"
  target_key_id = aws_kms_key.this.key_id
}

resource "aws_ecr_repository" "this" {
  for_each = local.ecr_container_repositories

  name                 = each.value
  image_tag_mutability = "MUTABLE"

  # TEMP for KMS migration: allow Terraform to delete non-empty repos
  force_delete         = true

  encryption_configuration {
    encryption_type = "KMS"
    kms_key = aws_kms_key.this.arn
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

