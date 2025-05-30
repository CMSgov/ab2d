data "aws_security_group" "api" {
  name   = "${local.service_prefix}-api-sg"
  vpc_id = local.vpc_id
}

data "aws_security_group" "worker" {
  name   = "${local.service_prefix}-worker-sg"
  vpc_id = local.vpc_id
}

data "aws_security_group" "lambda" {
  name   = "${local.service_prefix}-microservice-lambda-sg"
  vpc_id = local.vpc_id
}

data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}

data "aws_ssm_parameter" "database_name" { #TODO: this is derivable
  name = "/ab2d/${local.env}/core/nonsensitive/database_name"
}

data "aws_ssm_parameter" "database_password" {
  name = "/ab2d/${local.env}/core/sensitive/database_password"
}

data "aws_ssm_parameter" "database_user" {
  name = "/ab2d/${local.env}/core/sensitive/database_user"
}

data "aws_ssm_parameter" "keystore_password" {
  name = "/ab2d/${local.env}/core/sensitive/keystore_password"
}

data "aws_ssm_parameter" "keystore_location" { #TODO: this appears to be an invariant
  name = "/ab2d/${local.env}/core/nonsensitive/keystore_location"
}

data "aws_ssm_parameter" "okta_jwt_issuer" {
  name = "/ab2d/${local.env}/core/sensitive/okta_jwt_issuer"
}

data "aws_ssm_parameter" "slack_alert_webhooks" {
  name = "/ab2d/${local.env}/common/sensitive/slack_alert_webhooks"
}

data "aws_ssm_parameter" "slack_trace_webhooks" {
  name = "/ab2d/${local.env}/common/sensitive/slack_trace_webhooks"
}

###### Contracts Configuration #########
data "aws_ssm_parameter" "hpms_url" {
  name = "/ab2d/${local.env}/core/sensitive/hpms_url"
}

data "aws_ssm_parameter" "hpms_api_params" {
  name = "/ab2d/${local.env}/core/sensitive/hpms_api_params"
}

data "aws_ssm_parameter" "hpms_auth_key_id" {
  name = "/ab2d/${local.env}/core/sensitive/hpms_auth_key_id"
}

data "aws_ssm_parameter" "hpms_auth_key_secret" {
  name = "/ab2d/${local.env}/core/sensitive/hpms_auth_key_secret"
}

data "aws_iam_role" "task_execution_role" {
  name = "ab2d-${local.env}-microservice-task-definition-role"
}

data "aws_ecr_image" "contracts_service_image" {
  repository_name = "ab2d-contracts"
  image_tag       = var.contracts_service_image_tag
  most_recent     = var.contracts_service_image_tag == null ? true : null

}

data "aws_ecr_image" "events_service_image" {
  repository_name = "ab2d-events"
  image_tag       = var.events_service_image_tag
  most_recent     = var.events_service_image_tag == null ? true : null
}

data "aws_ecr_image" "properties_service_image" {
  repository_name = "ab2d-properties"
  image_tag       = var.properties_service_image_tag
  most_recent     = var.properties_service_image_tag == null ? true : null
}
