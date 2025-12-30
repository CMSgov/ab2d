data "aws_efs_file_system" "this" {
  file_system_id = module.platform.ssm.core.efs_file_system_id.value
}

data "aws_efs_access_point" "this" {
  access_point_id = module.platform.ssm.core.efs_access_point_id.value
}

data "aws_rds_cluster" "this" {
  cluster_identifier = local.service_prefix
}

data "aws_security_group" "db" {
  tags = {
    Name = "${local.service_prefix}-db"
  }
  vpc_id = local.vpc_id
}

data "aws_security_group" "efs" {
  name   = "ab2d-${local.parent_env}-efs"
  vpc_id = local.vpc_id
}

data "aws_security_group" "worker" {
  name = "${local.service_prefix}-worker"
}

data "aws_ecr_image" "worker" {
  repository_name = "ab2d-worker"
  image_tag       = var.worker_service_image_tag
  most_recent     = var.worker_service_image_tag == null ? true : null
}

data "aws_ssm_parameter" "bfd_mtls_private_key" {
  name = "/ab2d/${local.env}/api/sensitive/tls_private_key"
}

data "aws_ssm_parameter" "bfd_mtls_public_cert" {
  name = "/ab2d/${local.env}/api/nonsensitive/tls_public_cert"
}
