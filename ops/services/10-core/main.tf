terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source    = "git::https://github.com/CMSgov/cdap.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = local.app
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/10-core"
  service     = local.service
  ssm_root_map = {
    core = "/ab2d/${local.env}/core/"
    splunk = "/ab2d/mgmt/splunk/"
  }
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "core"

  benv = lookup({
    "test"    = "impl"
    "sandbox" = "sbx"
  }, local.parent_env, local.parent_env)

  database_user      = module.platform.ssm.core.database_user.value
  database_password  = module.platform.ssm.core.database_password.value
  aws_account_number = nonsensitive(module.platform.aws_caller_identity.account_id)
  env_key_alias      = module.platform.kms_alias_primary
  private_subnets    = nonsensitive(toset(keys(module.platform.private_subnets)))
  region_name        = module.platform.primary_region.name
  vpc_id             = module.platform.vpc_id
  splunk_alert_email = lookup(module.platform.ssm.splunk, "alert-email", { value : null }).value
}

resource "aws_s3_bucket" "main_bucket" {
  bucket_prefix = "${local.service_prefix}-main"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "main_bucket" {
  bucket = aws_s3_bucket.main_bucket.bucket
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.env_key_alias.target_key_arn
      sse_algorithm     = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_logging" "main_bucket" {
  bucket        = aws_s3_bucket.main_bucket.bucket
  target_bucket = module.platform.logging_bucket.id
  target_prefix = aws_s3_bucket.main_bucket.bucket_prefix
}

resource "aws_s3_bucket_versioning" "main_bucket" {
  bucket = aws_s3_bucket.main_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_ssm_parameter" "main_bucket" {
  name  = "/ab2d/${local.env}/core/nonsensitive/main-bucket-name"
  value = aws_s3_bucket.main_bucket.id
  type  = "String"
}

resource "aws_s3_bucket_public_access_block" "main_bucket" {
  bucket                  = aws_s3_bucket.main_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}


data "aws_efs_file_system" "efs" {
  count = module.platform.is_ephemeral_env ? 1 : 0

  creation_token = "ab2d-${local.parent_env}-efs"
}

data "aws_efs_access_points" "efs" {
  count = module.platform.is_ephemeral_env ? 1 : 0

  file_system_id = data.aws_efs_file_system.efs[0].file_system_id
}

resource "aws_efs_file_system" "efs" {
  count          = module.platform.is_ephemeral_env ? 0 : 1
  creation_token = "${local.service_prefix}-efs"
  encrypted      = "true"
  kms_key_id     = local.env_key_alias.target_key_arn
  tags           = { Name = "${local.service_prefix}-efs" }
}

resource "aws_efs_access_point" "efs" {
  count          = module.platform.is_ephemeral_env ? 0 : 1
  file_system_id = aws_efs_file_system.efs[0].id
  tags           = { Name = local.service_prefix }

  posix_user {
    gid = 0
    uid = 0
  }

  root_directory {
    path = "/"
    creation_info {
      permissions = 777
      owner_uid   = 0
      owner_gid   = 0
    }
  }
}

resource "aws_ssm_parameter" "efs_file_system" {
  name  = "/ab2d/${local.env}/core/nonsensitive/efs_file_system_id"
  value = module.platform.is_ephemeral_env ? data.aws_efs_file_system.efs[0].id : aws_efs_file_system.efs[0].id
  type  = "String"
}

resource "aws_ssm_parameter" "efs_access_point" {
  name  = "/ab2d/${local.env}/core/nonsensitive/efs_access_point_id"
  value = module.platform.is_ephemeral_env ? one(data.aws_efs_access_points.efs[0].ids) : aws_efs_access_point.efs[0].id
  type  = "String"
}

resource "aws_security_group" "efs" {
  count = module.platform.is_ephemeral_env ? 0 : 1

  name        = "${local.service_prefix}-efs"
  description = "EFS"
  vpc_id      = module.platform.vpc_id
  tags        = { Name = "${local.service_prefix}-efs" }
}

resource "aws_efs_mount_target" "this" {
  for_each = module.platform.is_ephemeral_env ? [] : local.private_subnets

  file_system_id  = aws_efs_file_system.efs[0].id
  subnet_id       = each.value
  security_groups = [aws_security_group.efs[0].id]
}

resource "aws_sns_topic" "efs" {
  count = module.platform.is_ephemeral_env ? 0 : 1

  name              = "${local.service_prefix}-efs-connections"
  kms_master_key_id = local.env_key_alias.target_key_id
}

resource "aws_cloudwatch_metric_alarm" "efs_health" {
  count = module.platform.is_ephemeral_env ? 0 : 1

  alarm_name          = "${local.service_prefix}-efs-connections"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "ClientConnections"
  namespace           = "AWS/EFS"
  period              = "60"
  statistic           = "Maximum"
  threshold           = "1"
  alarm_description   = "EFS connection count"
  treat_missing_data  = "ignore"
  alarm_actions       = [aws_sns_topic.efs[0].arn]
  ok_actions          = [aws_sns_topic.efs[0].arn]

  dimensions = {
    FileSystemId = aws_efs_file_system.efs[0].id
  }
}

resource "aws_sns_topic_subscription" "splunk_efs" {
  count     = local.splunk_alert_email != null ? 1 : 0
  topic_arn = aws_sns_topic.efs[0].arn
  protocol  = "email"
  endpoint  = local.splunk_alert_email
}

resource "aws_sns_topic" "alarms" {
  name = "${local.service_prefix}-cloudwatch-alarms"

  #FIXME this requires adjustments to the local.kms_master_key key policy
  # kms_master_key_id = local.kms_master_key_id
  kms_master_key_id = "alias/aws/sns"
}

resource "aws_sns_topic" "this" {
  name              = "${local.service_prefix}-events"
  kms_master_key_id = local.env_key_alias.target_key_id
}

resource "aws_sqs_queue" "this" {
  name                      = "${local.service_prefix}-events"
  delay_seconds             = 0
  max_message_size          = 262100
  message_retention_seconds = 86400
  receive_wait_time_seconds = 0
  kms_master_key_id         = local.env_key_alias.target_key_id
}

data "aws_iam_policy_document" "sqs" {
  statement {
    sid       = "First"
    effect    = "Allow"
    actions   = ["sqs:*"]
    resources = [aws_sqs_queue.this.arn]
  }
  statement {
    sid    = "SNSSubscription"
    effect = "Allow"
    actions = [
      "sqs:SendMessage"
    ]
    resources = [
      aws_sqs_queue.this.arn
    ]
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.this.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "this" {
  queue_url = aws_sqs_queue.this.id
  policy    = data.aws_iam_policy_document.sqs.json
}

resource "aws_security_group" "api" {
  name        = "${local.service_prefix}-api"
  description = "API security group"
  vpc_id      = local.vpc_id
  tags = {
    Name = "${local.service_prefix}-api"
  }
}

resource "aws_security_group_rule" "api_egress" {
  type              = "egress"
  description       = "${local.service_prefix} outbound connections"
  from_port         = "0"
  to_port           = "0"
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.api.id
}

resource "aws_security_group" "worker" {
  name        = "${local.service_prefix}-worker"
  description = "Worker security group"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.service_prefix}-worker" }
}

resource "aws_security_group" "lambda" {
  name        = "${local.service_prefix}-microservices-lambda"
  description = "Lambdas that need access to microservices security group"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.service_prefix}-microservices-lambda" }
}
