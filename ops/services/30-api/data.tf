data "aws_sqs_queue" "events" {
  name = "${local.service_prefix}-events"
}

data "aws_security_group" "efs" {
  name   = "ab2d-${local.parent_env}-efs"
  vpc_id = local.vpc_id
}

data "aws_security_group" "api" {
  name = "${local.service_prefix}-api"
}

data "aws_efs_file_system" "this" {
  file_system_id = module.platform.ssm.core.efs_file_system_id.value
}

data "aws_efs_access_point" "this" {
  access_point_id = module.platform.ssm.core.efs_access_point_id.value
}

data "aws_db_instance" "this" {
  count                  = contains(["prod"], local.parent_env) ? 1 : 0
  db_instance_identifier = local.service_prefix
}

data "aws_rds_cluster" "this" {
  count              = contains(["dev", "test", "sandbox"], local.parent_env) ? 1 : 0
  cluster_identifier = local.service_prefix
}

data "aws_ecr_image" "api" {
  repository_name = "ab2d-api"
  image_tag       = var.api_service_image_tag
  most_recent     = var.api_service_image_tag == null ? true : null
}

data "aws_sns_topic" "cloudwatch_alarms" {
  name = "${local.service_prefix}-cloudwatch-alarms"
}
