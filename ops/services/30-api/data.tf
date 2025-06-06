data "aws_ami" "ab2d" {
  most_recent = true
  owners      = ["self"]
  filter {
    name   = "tag:Name"
    values = ["ab2d-*-ami"]
  }
}

data "aws_ami" "cms" {
  most_recent = true
  owners      = [local.aws_account_cms_gold_images]
  filter {
    name   = "name"
    values = ["al2023-*"]
  }
}

data "aws_acm_certificate" "issued" {
  #TODO think harder about this withe ephemeral environments
  count     = module.platform.is_ephemeral_env ? 0 : 1
  domain    = "${local.env}.ab2d.cms.gov"
  statuses  = ["ISSUED", "EXPIRED"]
  key_types = ["RSA_2048", "RSA_4096"]
}

data "aws_sqs_queue" "events" {
  name = "${local.service_prefix}-events-sqs" #FIXME just use -events
}

data "aws_security_group" "efs" {
  #TODO Expand the filtering criteria to support ephemeral environments in the future
  name   = "${local.service_prefix}-efs-sg" #FIXME just use -efs
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

data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}

data "aws_ecr_image" "api" {
  repository_name = "ab2d-api"
  image_tag       = var.api_service_image_tag
  most_recent     = var.api_service_image_tag == null ? true : null
}

data "aws_sns_topic" "cloudwatch_alarms" {
  name = "${local.service_prefix}-cloudwatch-alarms"
}
