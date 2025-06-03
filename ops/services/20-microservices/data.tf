data "aws_sns_topic" "events" {
  name = "${local.service_prefix}-events"
}

data "aws_sqs_queue" "events" {
  name = "${local.service_prefix}-events-sqs" #FIXME just use -events
}

data "aws_security_group" "api" {
  name   = "${local.service_prefix}-api"
  vpc_id = local.vpc_id
}

data "aws_security_group" "worker" {
  name   = "${local.service_prefix}-worker"
  vpc_id = local.vpc_id
}

data "aws_security_group" "lambda" {
  name   = "${local.service_prefix}-microservices-lambda"
  vpc_id = local.vpc_id
}

data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}

data "aws_iam_role" "task_execution_role" {
  name = "${local.service_prefix}-microservices"
}

data "aws_ecr_image" "contracts" {
  repository_name = "ab2d-contracts"
  image_tag       = var.contracts_service_image_tag
  most_recent     = var.contracts_service_image_tag == null ? true : null
}

data "aws_ecr_image" "events" {
  repository_name = "ab2d-events"
  image_tag       = var.events_service_image_tag
  most_recent     = var.events_service_image_tag == null ? true : null
}

data "aws_ecr_image" "properties" {
  repository_name = "ab2d-properties"
  image_tag       = var.properties_service_image_tag
  most_recent     = var.properties_service_image_tag == null ? true : null
}
