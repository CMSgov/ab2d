terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.1"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "4.38.0"
    }
  }
  required_version = "~> 1.0"
}


provider "docker" {
}

provider "aws" {
  access_key = "mock_access_key"
  region     = "us-east-1"

  s3_use_path_style           = true
  secret_key                  = "mock_secret_key"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    sqs              = "http://host.docker.internal:4566"
    sns              = "http://host.docker.internal:4566"
    lambda           = "http://host.docker.internal:4566"
    iam              = "http://host.docker.internal:4566"
    kinesis          = "http://host.docker.internal:4566"
    cloudwatchevents = "http://host.docker.internal:4566"
  }
}

resource "aws_iam_role" "iam_for_everything" {
  name               = "iam"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Stmt1572416334166",
      "Action": "*",
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_cloudwatch_event_rule" "lambda_event_rule" {
  name                = "profile-generator-lambda-event-rule"
  description         = "retry scheduled every 2 min"
  schedule_expression = "rate(2 minutes)"
}


resource "aws_cloudwatch_event_target" "cloudwatch_audit_lambda_target" {
  arn  = aws_lambda_function.audit.arn
  rule = aws_cloudwatch_event_rule.lambda_event_rule.name
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_audit_lambda" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.audit.arn
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.lambda_event_rule.arn
}

#resource "aws_cloudwatch_event_rule" "lambda_ones_a_day_event_rule" {
#  name                = "profile-generator-lambda-ones-a-day-event-rule"
#  description         = "run at 12:00 am every day"
#  schedule_expression = "cron(0 0 * * ? *)"
#}
#
#resource "aws_cloudwatch_event_target" "cloudwatch_optout_lambda_target" {
#  arn  = aws_lambda_function.optout.arn
#  rule = aws_cloudwatch_event_rule.lambda_ones_a_day_event_rule.name
#}
#
#resource "aws_lambda_permission" "allow_cloudwatch_to_call_optout_lambda" {
#  statement_id  = "AllowExecutionFromCloudWatch"
#  action        = "lambda:InvokeFunction"
#  function_name = aws_lambda_function.optout.arn
#  principal     = "events.amazonaws.com"
#  source_arn    = aws_cloudwatch_event_rule.lambda_ones_a_day_event_rule.arn
#}

resource "aws_cloudwatch_event_rule" "profile_generator_lambda_event_rule" {
  name                = "profile-generator-lambda-event-rule"
  description         = "retry scheduled every 2 min"
  schedule_expression = "rate(2 minutes)"
}

resource "aws_cloudwatch_event_target" "profile_generator_lambda_target" {
  arn  = aws_lambda_function.audit.arn
  rule = aws_cloudwatch_event_rule.profile_generator_lambda_event_rule.name
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_rw_fallout_retry_step_deletion_lambda" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.audit.arn
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.profile_generator_lambda_event_rule.arn
}


resource "aws_lambda_function" "metrics" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip"
  function_name    = "CloudwatchEventHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.metrics.CloudwatchEventHandler"
  source_code_hash = filebase64sha256("/tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
    }
  }
  tags = {
    "key" = "lam"
  }
}

resource "aws_lambda_function" "audit" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/audit/build/distributions/audit.zip"
  function_name    = "AuditEventHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.audit.AuditEventHandler"
  source_code_hash = filebase64sha256("/tmp/setup/audit/build/distributions/audit.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
      environment                             = "local"
      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      AB2D_EFS_MOUNT                          = "/tmp/jobdownloads/"
      audit_files_ttl_hours                   = 1
    }
  }
  tags = {
    "key" = "lam"
  }
}

resource "aws_lambda_function" "coverage_count" {
  depends_on = [
    aws_iam_role.iam_for_everything, aws_lambda_function.database_management,
    data.aws_lambda_invocation.update_database_schema
  ]
  filename         = "/tmp/setup/coverage-counts/build/distributions/coverage-counts.zip"
  function_name    = "CoverageCountsHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.coveragecounts.CoverageCountsHandler"
  source_code_hash = filebase64sha256("/tmp/setup/coverage-counts/build/distributions/coverage-counts.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
      environment                             = "local"
      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      DB_URL                                  = "jdbc:postgresql://host.docker.internal:5432/ab2d"
      DB_USERNAME                             = "ab2d"
      DB_PASSWORD                             = "ab2d"
    }
  }
  tags = {
    "key" = "lam"
  }
}

resource "aws_sns_topic" "coverage_count_sns" {
  name = "local-coverage-count"
}

resource "aws_sns_topic_subscription" "user_updates_lampda_target" {
  topic_arn = aws_sns_topic.coverage_count_sns.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.coverage_count.arn
}

resource "aws_lambda_function" "database_management" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/database-management/build/distributions/database-management.zip"
  function_name    = "DatabaseManagementHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.databasemanagement.DatabaseManagementHandler"
  source_code_hash = filebase64sha256("/tmp/setup/database-management/build/distributions/database-management.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
      environment                             = "local"
      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      DB_URL                                  = "jdbc:postgresql://host.docker.internal:5432/ab2d"
      DB_USERNAME                             = "ab2d"
      DB_PASSWORD                             = "ab2d"
      LIQUIBASE_DUPLICATE_FILE_MODE           = "WARN"
      liquibaseSchemaName                     = "lambda"
    }
  }
  tags = {
    "key" = "lam"
  }
}

resource "aws_cloudwatch_event_target" "cloudwatch_hpms_lambda_target" {
  arn  = aws_lambda_function.hpms_counts.arn
  rule = aws_cloudwatch_event_rule.lambda_event_rule.name
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_hpms_lambda" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.hpms_counts.arn
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.lambda_event_rule.arn
}

resource "aws_lambda_function" "hpms_counts" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/retrieve-hpms-counts/build/distributions/retrieve-hpms-counts.zip"
  function_name    = "HPMSCountsHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.retrievehpmscounts.HPMSCountsHandler"
  source_code_hash = filebase64sha256("/tmp/setup/retrieve-hpms-counts/build/distributions/retrieve-hpms-counts.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
      environment                             = "local"
      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      "contract_service_url"                  = "http://host.docker.internal:8070"
    }
  }
  tags = {
    "key" = "lam"
  }
}

#resource "aws_lambda_function" "optout" {
#  depends_on       = [aws_iam_role.iam_for_everything]
#  filename         = "/tmp/setup/optout/build/distributions/optout.zip"
#  function_name    = "OptOutHandler"
#  role             = aws_iam_role.iam_for_everything.arn
#  handler          = "gov.cms.ab2d.optout.OptOutHandler"
#  source_code_hash = filebase64sha256("/tmp/setup/optout/build/distributions/optout.zip")
#  runtime          = "java11"
#  environment {
#    variables = {
#      "com.amazonaws.sdk.disableCertChecking" = true
#      IS_LOCALSTACK                           = true
#      environment                             = "local"
#      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
#      DB_URL                                  = "jdbc:postgresql://host.docker.internal:5432/ab2d"
#      DB_USERNAME                             = "ab2d"
#      DB_PASSWORD                             = "ab2d"
#    }
#  }
#  tags = {
#    "key" = "lam"
#  }
#}


