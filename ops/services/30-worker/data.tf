data "aws_default_tags" "this" {}

data "aws_efs_file_system" "this" {
  tags = {
    Name = "${local.service_prefix}-efs"
  }
}

data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}

data "aws_security_group" "db" {
  tags = {
    Name = "${local.service_prefix}-db"
  }
  vpc_id = local.vpc_id
}

data "aws_security_group" "efs" {
  name   = "${local.service_prefix}-efs-sg" #FIXME just use -efs
  vpc_id = local.vpc_id
}

data "aws_security_group" "worker" {
  name = "${local.service_prefix}-worker"
}

data "aws_ami" "ab2d_ami" {
  most_recent = true
  owners      = ["self"]
  filter {
    name   = "tag:Name"
    values = ["ab2d-*-ami"]
  }
}

data "aws_ami" "cms_gold" {
  most_recent = true
  owners      = [local.aws_account_cms_gold_images]
  filter {
    name   = "name"
    values = ["al2023-*"]
  }
}

data "aws_ecr_image" "worker" {
  repository_name = "ab2d-worker"
  image_tag       = var.worker_service_image_tag
  most_recent     = var.worker_service_image_tag == null ? true : null
}
