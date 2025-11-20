terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

locals {
  app              = "ab2d"
  established_envs = ["test", "prod"]
  parent_env = coalesce(
    var.parent_env,
    contains(local.established_envs, terraform.workspace) ? terraform.workspace : null,
    "invalid-parent-environment;do-better"
  )

  state_buckets = {
    test = "ab2d-test-tfstate-20250410134820763500000001"
    prod = "ab2d-prod-tfstate-20250411202936776600000001"
  }
}

variable "region" {
  default  = "us-east-1"
  nullable = false
}

variable "secondary_region" {
  default  = "us-west-2"
  nullable = false
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
    condition     = var.parent_env == null || one([for x in local.established_envs : x if var.parent_env == x && x == terraform.workspace]) != null
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
    use_lockfile = true
  }
}
