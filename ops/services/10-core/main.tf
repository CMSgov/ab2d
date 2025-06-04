terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = local.app
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/10-core"
  service     = local.service
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "core"

  aws_account_number = nonsensitive(module.platform.aws_caller_identity.account_id)
  env_key_alias      = module.platform.kms_alias_primary
  private_subnets    = nonsensitive(toset(keys(module.platform.private_subnets)))
  region_name        = module.platform.primary_region.name
  vpc_id             = module.platform.vpc_id
}

resource "aws_s3_bucket" "main_bucket" {
  bucket_prefix = "${local.service_prefix}-main"
  lifecycle {
    prevent_destroy = true
  }
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

resource "aws_efs_file_system" "efs" {
  creation_token = "${local.service_prefix}-efs"
  encrypted      = "true"
  kms_key_id     = local.env_key_alias.target_key_arn
  tags           = { "Name" = "${local.service_prefix}-efs" }
}

resource "aws_security_group" "efs" {
  name        = "${local.service_prefix}-efs-sg"
  description = "EFS"
  vpc_id      = module.platform.vpc_id
  tags        = { "Name" = "${local.service_prefix}-efs-sg" }
}

resource "aws_efs_mount_target" "this" {
  for_each        = local.private_subnets
  file_system_id  = aws_efs_file_system.efs.id
  subnet_id       = each.value
  security_groups = [aws_security_group.efs.id]
}

resource "aws_sns_topic" "efs" {
  name              = "${local.service_prefix}-efs-connections"
  kms_master_key_id = local.env_key_alias.target_key_id
}

resource "aws_cloudwatch_metric_alarm" "efs_health" {
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
  alarm_actions       = [aws_sns_topic.efs.arn]
  ok_actions          = [aws_sns_topic.efs.arn]

  dimensions = {
    FileSystemId = aws_efs_file_system.efs.id
  }
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
  #FIXME gov.cms.ab2d.eventclient.clients hard-codes suffix of "-events-sqs" 😕
  name                      = "${local.service_prefix}-events-sqs"
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
    "Name" = "${local.service_prefix}-api"
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

resource "aws_security_group_rule" "db_access_api" {
  type                     = "ingress"
  description              = "${local.service_prefix} api connections"
  from_port                = "5432"
  to_port                  = "5432"
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.api.id
  security_group_id        = data.aws_security_group.db.id
}

resource "aws_security_group" "worker" {
  name        = "${local.service_prefix}-worker"
  description = "Worker security group"
  vpc_id      = local.vpc_id
  tags        = { "Name" = "${local.service_prefix}-worker" }
}

resource "aws_security_group" "lambda" {
  name        = "${local.service_prefix}-microservices-lambda"
  description = "Lambdas that need access to microservices security group"
  vpc_id      = local.vpc_id
  tags        = { "Name" = "${local.service_prefix}-microservices-lambda" }
}
