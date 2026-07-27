data "aws_ecr_repository" "idr_db_importer" {
  name = "${local.app}-${local.service}"
}

data "aws_ecs_cluster" "shared" {
  cluster_name = local.service_prefix
}

data "aws_rds_cluster" "this" {
  cluster_identifier = local.service_prefix
}

### SSM parameters

data "aws_ssm_parameter" "ab2d_db_database" {
  name = "/ab2d/${local.env}/core/nonsensitive/database_name"
}

data "aws_ssm_parameter" "ab2d_db_host" {
  name = "/ab2d/${local.env}/core/nonsensitive/writer_endpoint"
}

data "aws_ssm_parameter" "ab2d_db_password" {
  name = "/ab2d/${local.env}/core/sensitive/database_password"
}

data "aws_ssm_parameter" "ab2d_db_user" {
  name = "/ab2d/${local.env}/core/sensitive/database_user"
}

# data "aws_ssm_parameter" "ab2d_aurora_database_security_group" {
#   name = "/ab2d/${local.env}/aurora/nonsensitive/db-security-group-id"
# }
#
# data "aws_ssm_parameter" "ab2d_aurora_writer_endpoint_id" {
#   name = "/ab2d/${local.env}/aurora/nonsensitive/writer-endpoint"
# }