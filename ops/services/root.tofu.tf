# This root tofu.tf is symlink'd to by all per-env Terraservices. Changes to this tofu.tf apply to
# _all_ Terraservices, so be careful!

locals {
  app              = "ab2d"
  established_envs = ["dev", "test", "sandbox", "prod"]
  service_prefix   = "${local.app}-${local.env}"

  parent_env = coalesce(
    var.parent_env,
    one([for x in local.established_envs : x if can(regex("${x}$$", terraform.workspace))]),
    "invalid-parent-environment;do-better"
  )

  state_buckets = {
    dev     = "ab2d-dev-tfstate-20250417141439646700000001"
    test    = "ab2d-test-tfstate-20250410134820763500000001"
    sandbox = "ab2d-sandbox-tfstate-20250416200059224300000001"
    prod    = "ab2d-prod-tfstate-20250411202936776600000001"
  }
}

variable "region" {
  default  = "us-east-1"
  nullable = false
  type     = string
}

variable "secondary_region" {
  default  = "us-west-2"
  nullable = false
  type     = string
}

variable "parent_env" {
  description = <<-EOF
  The parent environment of the current solution. Will correspond with `terraform.workspace`".
  Necessary on `tofu init` and `tofu workspace select` _only_. In all other situations, parent env
  will be divined from `terraform.workspace`.
  EOF
  type        = string
  nullable    = true
  default     = null
  validation {
    condition     = var.parent_env == null || one([for x in local.established_envs : x if var.parent_env == x && endswith(terraform.workspace, x)]) != null
    error_message = "Invalid parent environment name."
  }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = local.default_tags
  }
}

provider "aws" {
  alias = "secondary"

  region = var.secondary_region
  default_tags {
    tags = local.default_tags
  }
}

terraform {
  backend "s3" {
    bucket       = local.state_buckets[local.parent_env]
    key          = "ops/services/${local.service}/tofu.tfstate"
    region       = var.region
    encrypt      = true
    kms_key_id   = "alias/${local.app}-${local.parent_env}-tfstate-bucket"
    use_lockfile = true
  }
}
