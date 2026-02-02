data "aws_ecr_repository" "idr_db_importer" {
  name = "${local.app}-${local.service}"
}

data "aws_ecs_cluster" "shared" {
  cluster_name = "${local.app}-${local.env}"
}

data "aws_iam_role" "idr_db_importer_task" {
  name = "${local.service_prefix}-${local.service}-task"
}

data "aws_iam_role" "idr_db_importer_task_execution" {
  name = "${local.service_prefix}-${local.service}-task-execution"
}

data "aws_security_group" "idr_db_importer_eventbridge_scheduler" {
  name = "${local.service_prefix}-idr-db-importer-eventbridge-scheduler"
}

### SSM parameters

data "aws_ssm_parameter" "ab2d_db_database" {
  name = "/ab2d/test/core/nonsensitive/database_name"
}

data "aws_ssm_parameter" "ab2d_db_host" {
  name = "/ab2d/test/core/nonsensitive/writer_endpoint"
}

data "aws_ssm_parameter" "ab2d_db_password" {
  name = "/ab2d/test/core/sensitive/database_password"
}

data "aws_ssm_parameter" "ab2d_db_user" {
  name = "/ab2d/test/core/sensitive/database_user"
}

data "aws_ssm_parameter" "idr_db_importer_bucket" {
  name = "/ab2d/test/core/nonsensitive/idr-db-importer-bucket"
}