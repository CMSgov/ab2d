terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
    artifactory = {
      source  = "jfrog/artifactory"
      version = "12.9.4"
    }
  }
}

module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = local.app
  env          = local.env
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-lambda"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "lambda"

  ssm_root_map = {
    artifactory   = "/ab2d/mgmt/artifactory"
    microservices = "/ab2d/${local.env}/microservices"
    common        = "/ab2d/${local.env}/common"
    core          = "/ab2d/${local.env}/core"
    webhooks      = "/ab2d/mgmt/slack-webhooks"
  }

  db_host = data.aws_db_instance.this.address

  java_options          = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
  efs_mount             = "/mnt/efs"
  audit_schedule        = "2 hours"
  ndjson_ttl            = 72
  efs_security_group_id = data.aws_security_group.efs_sg.id
  vpc_id                = module.platform.vpc_id
  node_subnet_ids       = keys(module.platform.private_subnets)
  db_sg_id              = data.aws_security_group.db.id
  db_port               = 5432
  ab2d_db_ssl_mode      = "allow"

  db_name                         = module.platform.ssm.core.database_name.value
  db_password                     = module.platform.ssm.core.database_password.value
  db_username                     = module.platform.ssm.core.database_user.value
  slack_webhook_ab2d_slack_alerts = module.platform.ssm.webhooks.ab2d-slack-alerts.value
  contracts_service_url           = module.platform.ssm.microservices.url.value

  microservices_lb = data.aws_security_group.microservices_lb.id
  cloudfront_id    = data.aws_ec2_managed_prefix_list.cloudfront.id

  hpms_counts_schedule = "7 days" # I don't see a "1 week" option
  release_version      = "1.1.0"
  lambdas = toset([
    "metrics-lambda",
    "audit",
    "database-management",
    "coverage-counts",
    "retrieve-hpms-counts",
  ])
}

provider "artifactory" {
  url          = module.platform.ssm.artifactory.url.value
  access_token = module.platform.ssm.artifactory.token.value
}

resource "aws_lambda_function" "slack_lambda" {
  filename      = "${path.root}/slack_lambda.zip"
  function_name = "${local.service_prefix}-slack-alerts"
  role          = aws_iam_role.slack_lambda.arn
  handler       = "data_load_lambda.load_data"
  runtime       = "python3.12"
  timeout       = 600
  environment {
    variables = {
      SLACK_WEBHOOK_URL = local.slack_webhook_ab2d_slack_alerts
    }
  }
  tags = {
    code = "https://github.com/CMSgov/ab2d/blob/main/ops/services/30-lambda/code/lambda_function.py"
  }
}

resource "aws_lambda_function" "metrics_transform" {
  filename         = "${path.root}/metrics-lambda.zip"
  source_code_hash = base64encode(data.artifactory_file.lambdas["metrics-lambda"].sha256)
  function_name    = "${local.service_prefix}-metrics-transform"
  role             = aws_iam_role.metrics_transform.arn
  handler          = "gov.cms.ab2d.metrics.CloudwatchEventHandler"
  runtime          = "java11"
  memory_size      = 256
  timeout          = 600
  environment {
    variables = {
      environment       = local.env
      JAVA_TOOL_OPTIONS = local.java_options
    }
  }
  tags = { code = "https://github.com/CMSgov/ab2d-lambdas/tree/main/metrics-lambda" }
}

resource "aws_lambda_function" "audit" {
  filename         = "${path.root}/audit.zip"
  source_code_hash = base64encode(data.artifactory_file.lambdas["audit"].sha256)
  function_name    = "${local.service_prefix}-audit"
  role             = aws_iam_role.microservices_lambda.arn
  handler          = "gov.cms.ab2d.audit.AuditEventHandler"
  runtime          = "java11"
  timeout          = 600
  vpc_config {
    security_group_ids = [local.efs_security_group_id, aws_security_group.audit_lambda.id]
    subnet_ids         = local.node_subnet_ids
  }
  file_system_config {
    arn              = data.aws_efs_access_point.this.arn
    local_mount_path = local.efs_mount
  }
  environment {
    variables = {
      environment           = local.env
      JAVA_TOOL_OPTIONS     = local.java_options
      AB2D_EFS_MOUNT        = local.efs_mount
      audit_files_ttl_hours = local.ndjson_ttl
    }
  }
  tags = { code = "https://github.com/CMSgov/ab2d-lambdas/tree/main/audit" }
}

resource "aws_security_group" "audit_lambda" {
  name        = "${local.service_prefix}-audit-lambda"
  description = "Audit Lambda security group"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.service_prefix}-audit-lambda" }
}

resource "aws_security_group_rule" "audit_efs_egress" {
  type                     = "egress"
  description              = "Allow ingress from audit lambda to efs"
  from_port                = "2049"
  to_port                  = "2049"
  protocol                 = "TCP"
  source_security_group_id = local.efs_security_group_id
  security_group_id        = aws_security_group.audit_lambda.id
}

resource "aws_security_group_rule" "efs_ingress" {
  type                     = "ingress"
  description              = "NFS"
  from_port                = 2049
  to_port                  = 2049
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.audit_lambda.id
  security_group_id        = local.efs_security_group_id
}

resource "aws_cloudwatch_event_rule" "audit_lambda_event_rule" {
  name                = "${local.service_prefix}-audit-lambda-event-rule"
  description         = "retry scheduled every ${local.audit_schedule}"
  schedule_expression = "rate(${local.audit_schedule})"
}

resource "aws_cloudwatch_event_target" "audit_lambda_target" {
  arn  = aws_lambda_function.audit.arn
  rule = aws_cloudwatch_event_rule.audit_lambda_event_rule.name
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_deletion_lambda" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.audit.arn
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.audit_lambda_event_rule.arn
}

#TODO determine if this dependency chain makes sense; if this errors on next creation, we will know that it's bad.
resource "aws_cloudwatch_log_group" "audit" {
  name              = "/aws/lambda/${aws_lambda_function.audit.function_name}"
  retention_in_days = 30
  tags = {
    Name = aws_lambda_function.audit.function_name
  }
}

resource "aws_lambda_permission" "audit_svc_monitoring" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.audit_svc_monitoring.function_name
  principal     = "logs.us-east-1.amazonaws.com" #TODO consider putting in a dynamic region?
  source_arn    = "${aws_cloudwatch_log_group.audit.arn}:*"
}

resource "aws_cloudwatch_log_subscription_filter" "audit_svc_monitoring" {
  destination_arn = aws_lambda_function.audit_svc_monitoring.arn
  filter_pattern  = "?ERROR ?Error ?Exception ?timed"
  log_group_name  = aws_cloudwatch_log_group.audit.name
  name            = aws_lambda_function.audit_svc_monitoring.function_name
}

resource "aws_lambda_function" "audit_svc_monitoring" {
  filename      = "${path.root}/monitoring_audit_svc.zip"
  function_name = "${local.service_prefix}-audit-svc-monitoring"
  handler       = "monitoring_audit_svc.lambda_handler"
  role          = aws_iam_role.microservices_lambda.arn
  runtime       = "python3.12"
  timeout       = 600
  description   = "Lambda function that monitors the Audit srvice lambda and sends alert to slack"
  tags          = { code = "https://github.com/CMSgov/ab2d/blob/main/ops/services/30-lambda/code/monitoring_audit_svc.py" }
}

resource "aws_security_group_rule" "db_access_lambda_ingress" {
  type                     = "ingress"
  description              = "${local.env} lambda db connection ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.database_lambda.id
  security_group_id        = local.db_sg_id
}

resource "aws_security_group" "database_lambda" {
  name        = "${local.service_prefix}-database-lambda"
  description = "Lambdas that need database access security group"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.service_prefix}-database-lambda" }
}

resource "aws_lambda_function" "coverage_count" {
  depends_on = [
    aws_lambda_function.database_management,
    data.aws_lambda_invocation.update_database_schema
  ]
  vpc_config {
    security_group_ids = [local.db_sg_id, aws_security_group.database_lambda.id]
    subnet_ids         = local.node_subnet_ids
  }
  filename         = "${path.root}/coverage-counts.zip"
  function_name    = "${local.service_prefix}-coverage-counts-handler"
  role             = aws_iam_role.lambda_database_sns_role.arn
  handler          = "gov.cms.ab2d.coveragecounts.CoverageCountsHandler"
  source_code_hash = base64encode(data.artifactory_file.lambdas["coverage-counts"].sha256)
  runtime          = "java11"
  timeout          = 600
  memory_size      = 1024
  environment {
    variables = {
      environment       = local.env
      JAVA_TOOL_OPTIONS = local.java_options
      DB_URL            = "jdbc:postgresql://${local.db_host}:${local.db_port}/${local.db_name}?sslmode=${local.ab2d_db_ssl_mode}&reWriteBatchedInserts=true"
      DB_USERNAME       = local.db_username
      DB_PASSWORD       = local.db_password
    }
  }
  tags = {
    code = "https://github.com/CMSgov/ab2d-lambdas/tree/main/coverage-counts"
  }
  description = "Lambda function to record coverage counts from ab2d, bfd, and hpms"
}

resource "aws_sns_topic" "coverage_count_sns" {
  name = "${local.env}-coverage-count"
  #FIXME
  lifecycle {
    ignore_changes = [kms_master_key_id]
  }
}

resource "aws_lambda_permission" "coverage_count_sns_permission" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.coverage_count.function_name
  principal     = "sns.amazonaws.com"
  statement_id  = "AllowExecutionFromSNS"
  source_arn    = aws_sns_topic.coverage_count_sns.arn
}

resource "aws_sns_topic_subscription" "coverage_count_lambda_target" {
  topic_arn = aws_sns_topic.coverage_count_sns.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.coverage_count.arn
}

resource "aws_lambda_function" "database_management" {
  filename         = "${path.root}/database-management.zip"
  function_name    = "${local.service_prefix}-database-management-handler"
  role             = aws_iam_role.lambda_database_sns_role.arn
  handler          = "gov.cms.ab2d.databasemanagement.DatabaseManagementHandler"
  source_code_hash = base64encode(data.artifactory_file.lambdas["database-management"].sha256)
  runtime          = "java11"
  timeout          = 600
  memory_size      = 256
  vpc_config {
    security_group_ids = [local.db_sg_id, aws_security_group.database_lambda.id]
    subnet_ids         = local.node_subnet_ids
  }
  environment {
    variables = {
      environment                   = local.env
      JAVA_TOOL_OPTIONS             = local.java_options
      DB_URL                        = "jdbc:postgresql://${local.db_host}:${local.db_port}/${local.db_name}?sslmode=${local.ab2d_db_ssl_mode}&reWriteBatchedInserts=true"
      DB_USERNAME                   = local.db_username
      DB_PASSWORD                   = local.db_password
      LIQUIBASE_DUPLICATE_FILE_MODE = "WARN"
      liquibaseSchemaName           = "lambda"
    }
  }
  tags = {
    code = "https://github.com/CMSgov/ab2d-lambdas/tree/main/database-management"
  }
  description = "Lambda function that calls liquibase to manage our db tables"
}

resource "aws_lambda_function" "hpms_counts" {
  filename         = "${path.root}/retrieve-hpms-counts.zip"
  function_name    = "${local.service_prefix}-hpms-counts-handler"
  role             = aws_iam_role.lambda_sns_role.arn
  handler          = "gov.cms.ab2d.retrievehpmscounts.HPMSCountsHandler"
  source_code_hash = base64encode(data.artifactory_file.lambdas["retrieve-hpms-counts"].sha256)
  runtime          = "java11"
  timeout          = 600
  memory_size      = 256
  vpc_config {
    security_group_ids = [local.microservices_lb, data.aws_security_group.microservices_lambda.id]
    subnet_ids         = local.node_subnet_ids
  }
  environment {
    variables = {
      environment          = local.env
      JAVA_TOOL_OPTIONS    = local.java_options
      contract_service_url = local.contracts_service_url
      SLACK_WEBHOOK_URL    = local.slack_webhook_ab2d_slack_alerts
    }
  }
  tags = {
    code = "https://github.com/CMSgov/ab2d-lambdas/tree/main/retrieve-hpms-counts"
  }
  description = "Lambda function that calls hpms then forwards counts to coverage counts lambda"
}

resource "aws_security_group_rule" "contract_lambda_sg_ingress_access" {
  type                     = "ingress"
  from_port                = 8070
  to_port                  = 8070
  protocol                 = "tcp"
  description              = "contract to lambda"
  source_security_group_id = local.microservices_lb
  security_group_id        = data.aws_security_group.microservices_lambda.id
}
resource "aws_security_group_rule" "lambda_contract_egress_access" {
  type              = "egress"
  from_port         = 80
  to_port           = 443
  protocol          = "tcp"
  description       = "hpms lambda egress"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = data.aws_security_group.microservices_lambda.id
}

resource "aws_security_group_rule" "sns_lambda" {
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  description       = "lambda sns"
  prefix_list_ids   = [local.cloudfront_id]
  security_group_id = data.aws_security_group.microservices_lambda.id
}

resource "aws_cloudwatch_event_rule" "hpms_counts_lambda_event_rule" {
  name                = "${local.service_prefix}-hpms-counts-lambda-event-rule"
  description         = "retry scheduled every ${local.hpms_counts_schedule}"
  schedule_expression = "rate(${local.hpms_counts_schedule})"
}

resource "aws_cloudwatch_event_target" "hpms_counts_lambda_target" {
  arn  = aws_lambda_function.hpms_counts.arn
  rule = aws_cloudwatch_event_rule.hpms_counts_lambda_event_rule.name
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_hpms_counts_lambda" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.hpms_counts.arn
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.hpms_counts_lambda_event_rule.arn
}
