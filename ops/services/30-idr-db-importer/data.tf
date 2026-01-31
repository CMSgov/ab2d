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