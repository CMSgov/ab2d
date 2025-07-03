data "aws_sqs_queue" "events" {
  name = "${local.service_prefix}-events"
}

data "aws_security_group" "efs" {
  name   = "${local.service_prefix}-efs"
  vpc_id = local.vpc_id
}

data "aws_security_group" "api" {
  name = "${local.service_prefix}-api"
}

data "aws_efs_file_system" "this" {
  tags = {
    Name = "${local.service_prefix}-efs"
  }
}

data "aws_efs_access_point" "this" {
  access_point_id = module.platform.ssm.core.efs_access_point_id.value
}

data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}

data "aws_rds_cluster" "this" {
  cluster_identifier = "${local.service_prefix}-aurora"
}

data "aws_ecr_image" "api" {
  repository_name = "ab2d-api"
  image_tag       = var.api_service_image_tag
  most_recent     = var.api_service_image_tag == null ? true : null
}

data "aws_sns_topic" "cloudwatch_alarms" {
  name = "${local.service_prefix}-cloudwatch-alarms"
}
