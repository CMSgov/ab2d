#FIXME: challenge the below assumption... what is this?
/**
  Lambdas cost is based on execution time. Instead of calling liquibase on every function invoke we can it once for each deploy.
**/
data "aws_lambda_invocation" "update_database_schema" {
  depends_on    = [aws_lambda_function.database_management]
  function_name = aws_lambda_function.database_management.function_name
  input         = <<JSON
  {
  }
  JSON
}

#FIXME: Error: reading CloudWatch Logs Log Group (/aws/lambda/audit-dev): empty result
# data "aws_cloudwatch_log_group" "audit_svc_cw_log_group" {
#   name = "/aws/lambda/audit-${local.env}"
#   depends_on = [
#     aws_lambda_function.audit
#   ]
# }

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

data "archive_file" "python_lambda_package" {
  type        = "zip"
  source_file = "${path.module}/code/slack_lambda.py"
  output_path = "${path.module}/slack_lambda.zip"
}

data "archive_file" "monitoring_audit" {
  type        = "zip"
  source_file = "${path.module}/code/monitoring_audit_svc.py"
  output_path = "${path.module}/monitoring_audit_svc.zip"
}

data "artifactory_file" "lambdas" {
  for_each = local.lambdas

  repository  = "ab2d-main"
  path        = "gov/cms/ab2d/${each.value}/${local.release_version}/${each.value}-${local.release_version}.zip"
  output_path = "${path.module}/${each.value}.zip"
}

data "aws_efs_file_system" "efs" {
  tags = {
    Name = "ab2d-${module.platform.parent_env}-efs"
  }
}

data "aws_security_group" "microservices_lambda" {
  name = "${local.service_prefix}-microservices-lambda"
}

data "aws_security_group" "microservices_lb" {
  filter {
    name   = "tag:Name"
    values = ["${local.service_prefix}-microservices-lb"]
  }
}

data "aws_security_group" "efs_sg" {
  filter {
    name   = "tag:Name"
    values = ["ab2d-${module.platform.parent_env}-efs-sg"] #FIXME just use -efs
  }
}

data "aws_security_group" "db" {
  filter {
    name   = "tag:Name"
    values = ["ab2d-${local.env}-db"]
  }
}

data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}
