locals {
  export_schedule = lookup(
    {
      prod = "cron(0 1 ? * WED *)"
      test = "cron(0 13 ? * * *)"
  }, local.env, "")

  export_bucket = {
    prod = "ab2d-prod-opt-out-export-function-20250616154436478600000001"
    test = "ab2d-test-opt-out-export-function-20250529140617557400000001"
  }
}

data "aws_s3_object" "export" {
  for_each = toset([for env in ["prod", "test"] : env if env == local.env])

  bucket = local.export_bucket[each.key]
  key    = "function.zip"
}

resource "aws_iam_role_policy" "export_assume_bucket_role" {
  count = contains(["prod", "test"], local.env) ? 1 : 0
  name  = "assume-bucket-role"
  role  = aws_iam_role.opt_out[0].id
  policy = jsonencode({
    Statement = [
      {
        Action   = "sts:AssumeRole"
        Effect   = "Allow"
        Resource = module.platform.ssm.eft.bfd-bucket-role-arn.value
      }
    ]
    Version = "2012-10-17"
  })
}

resource "aws_lambda_function" "export" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  s3_bucket                      = data.aws_s3_object.export[local.env].bucket
  s3_key                         = data.aws_s3_object.export[local.env].key
  s3_object_version              = data.aws_s3_object.export[local.env].version_id
  description                    = "Exports data files to a BFD bucket for opt-out"
  function_name                  = "${local.service_prefix}-opt-out-export"
  handler                        = "gov.cms.ab2d.attributiondatashare.AttributionDataShareHandler"
  kms_key_arn                    = local.env_key_alias.target_key_arn
  memory_size                    = 10240
  timeout                        = 900
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = aws_iam_role.opt_out[0].arn
  runtime                        = "java17"
  skip_destroy                   = false
  tags = {
    code = "https://github.com/CMSgov/ab2d/tree/main/lambdas/optout"
  }

  environment {
    variables = {
      APP_NAME         = "${local.service_prefix}-opt-out-export"
      DB_HOST          = "jdbc:postgresql://${local.ab2d_db_host}:${local.db_port}/${local.db_name}?sslmode=${local.ab2d_db_ssl_mode}&ApplicationName=${local.service_prefix}-opt-out-export"
      ENV              = local.env
      S3_UPLOAD_BUCKET = "bfd-${local.env}-eft"
      S3_UPLOAD_PATH   = "bfdeft01/ab2d/out"
    }
  }

  ephemeral_storage {
    size = local.env == "prod" ? 10240 : 512
  }

  logging_config {
    log_format = "Text"
    log_group  = "/aws/lambda/${local.service_prefix}-opt-out-export"
  }

  tracing_config {
    mode = "PassThrough"
  }

  vpc_config {
    security_group_ids = [local.db_sg_id, aws_security_group.database_lambda.id]
    subnet_ids         = local.node_subnet_ids
  }
}

resource "aws_cloudwatch_event_rule" "export" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  name                = "${aws_lambda_function.export[0].function_name}-function"
  description         = "Trigger ${aws_lambda_function.export[0].function_name} function"
  schedule_expression = local.export_schedule
}

resource "aws_cloudwatch_event_target" "export" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  arn  = aws_lambda_function.export[0].arn
  rule = aws_cloudwatch_event_rule.export[0].name
}

resource "aws_lambda_permission" "export" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  statement_id  = "AllowExecutionFromCloudWatchEvents"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.export[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.export[0].arn
}
