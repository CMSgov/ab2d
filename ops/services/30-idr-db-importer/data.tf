data "aws_ecr_repository" "idr_db_importer" {
  name = "${local.app}-${local.service}"
}

data "aws_ecs_cluster" "shared" {
  cluster_name = "${local.app}-${local.env}"
}

data "aws_iam_role" "idr_db_importer" {
  name = "${local.service_prefix}-${local.service}"
}

data "aws_iam_role" "idr_db_importer_execution" {
  name = "${local.service_prefix}-${local.service}-execution"
}