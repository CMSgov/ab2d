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
    "ab2d-idr-db-importer",
    "ab2d-worker",
    "ab2d-contracts",
    "ab2d-events"
  ])
}

variable "create_local_sops_wrapper" {
  default = false
  type    = bool
}

module "sops" {
  source = "github.com/CMSgov/cdap//terraform/modules/sops?ref=38711d86419b9d91a003f4eea4931287d546b55b"

  create_local_sops_wrapper = var.create_local_sops_wrapper
  platform                  = module.platform
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

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.this.arn
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

data "aws_ecr_lifecycle_policy_document" "this" {
  rule {
    priority    = 1
    description = "Keep the most recent 15 images built from the main branch"
    selection {
      tag_status      = "tagged"
      tag_prefix_list = ["*-main-*"]
      count_type      = "imageCountMoreThan"
      count_number    = 15
    }
    action {
        type = "expire"
    }
  }

  rule {
    priority    = 2
    description = "Expire images older than 7 days created by pull-request workflows"
    selection {
      tag_status      = "tagged"
      tag_prefix_list = ["*-merge-*"]
      count_type      = "sinceImagePushed"
      count_unit      = "days"
      count_number    = 7
    }
    action {
        type = "expire"
    }
  }

  rule {
    priority    = 3
    description = "Expire all other images older than 21 days"
    selection {
      tag_status   = "any"
      count_type   = "sinceImagePushed"
      count_unit   = "days"
      count_number = 21
    }
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = local.ecr_container_repositories
  repository = each.value
  policy     = data.aws_ecr_lifecycle_policy_document.this.json
}
