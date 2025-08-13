locals {
  import_bucket = {
    prod = "ab2d-prod-opt-out-import-function-20250616155408985300000001"
    test = "ab2d-test-opt-out-import-function-20250529140353146500000001"
  }
}

data "aws_s3_object" "import" {
  for_each = toset([for env in ["prod", "test"] : env if env == local.env])

  bucket = local.import_bucket[each.key]
  key    = "function.zip"
}

resource "aws_iam_role" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0
  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "lambda.amazonaws.com"
          }
        },
        {
          Action = [
            "sts:TagSession",
            "sts:AssumeRoleWithWebIdentity",
          ]
          Condition = {
            StringEquals = {
              "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
            }
            StringLike = {
              "token.actions.githubusercontent.com:sub" = "repo:CMSgov/ab2d:*"
            }
          }
          Effect = "Allow"
          Principal = {
            Federated = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:oidc-provider/token.actions.githubusercontent.com"
          }
        },
        {
          Action = [
            "sts:TagSession",
            "sts:AssumeRole",
          ]
          Effect = "Allow"
          Principal = {
            AWS = [for role in module.platform.kion_roles : role.arn]
          }
        },
      ]
      Version = "2012-10-17"
    }
  )
  force_detach_policies = false
  max_session_duration  = 3600
  name                  = "${local.service_prefix}-opt-out-import-function"
  path                  = "/delegatedadmin/developer/"
  permissions_boundary  = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"

  inline_policy {
    name = "assume-bucket-role"
    policy = jsonencode(
      {
        Statement = [
          {
            Action   = "sts:AssumeRole"
            Effect   = "Allow"
            Resource = module.platform.ssm.eft.bfd-bucket-role-arn.value
          },
        ]
        Version = "2012-10-17"
      }
    )
  }

  inline_policy {
    name = "default-function"
    policy = jsonencode(
      {
        Version = "2012-10-17"
        Statement = [
          {
            Action = [
              "ssm:GetParameters",
              "ssm:GetParameter",
              "sqs:ReceiveMessage",
              "sqs:GetQueueAttributes",
              "sqs:DeleteMessage",
              "logs:PutLogEvents",
              "logs:CreateLogStream",
              "logs:CreateLogGroup",
              "ec2:DescribeNetworkInterfaces",
              "ec2:DescribeAccountAttributes",
              "ec2:DeleteNetworkInterface",
              "ec2:CreateNetworkInterface"
            ]
            Effect   = "Allow"
            Resource = "*"
          },
          {
            Action = [
              "kms:Encrypt",
              "kms:Decrypt"
            ]
            Effect   = "Allow"
            Resource = [local.env_key_alias.target_key_arn]
          }
        ]
      }
    )
  }
}

resource "aws_lambda_function" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0


  s3_bucket         = data.aws_s3_object.import[local.env].bucket
  s3_key            = data.aws_s3_object.import[local.env].key
  s3_object_version = data.aws_s3_object.import[local.env].version_id
  architectures = [
    "x86_64",
  ]
  description                    = "Ingests the most recent beneficiary opt-out list from BFD"
  function_name                  = "${local.service_prefix}-opt-out-import"
  handler                        = "gov.cms.ab2d.optout.OptOutHandler"
  kms_key_arn                    = local.env_key_alias.target_key_arn
  memory_size                    = 1024
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = aws_iam_role.import[0].arn
  runtime                        = "java11"
  skip_destroy                   = false

  tags = {
    code = "https://github.com/CMSgov/ab2d/tree/main/lambdas/optout"
  }
  timeout = 900

  environment {
    variables = {
      APP_NAME = "${local.service_prefix}-opt-out-import"
      DB_HOST  = "jdbc:postgresql://${local.ab2d_db_host}:${local.db_port}/${local.db_name}?sslmode=${local.ab2d_db_ssl_mode}&ApplicationName=${local.service_prefix}-opt-out-import"
      ENV      = local.env
    }
  }

  ephemeral_storage {
    size = local.env == "prod" ? 10240 : 512
  }

  logging_config {
    log_format = "Text"
    log_group  = "/aws/lambda/${local.service_prefix}-opt-out-import"
  }

  tracing_config {
    mode = "PassThrough"
  }

  vpc_config {
    security_group_ids = [local.db_sg_id, aws_security_group.database_lambda.id]
    subnet_ids         = local.node_subnet_ids
  }
}

resource "aws_sqs_queue" "dead_letter" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  name              = "${local.service_prefix}-opt-out-import-dead-letter"
  kms_master_key_id = local.env_key_alias.target_key_arn
}

resource "aws_sqs_queue" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  name              = "${local.service_prefix}-opt-out-import"
  kms_master_key_id = local.env_key_alias.target_key_arn

  visibility_timeout_seconds = 900

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dead_letter[0].arn
    maxReceiveCount     = 4
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  queue_url = aws_sqs_queue.dead_letter[0].id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.import[0].arn]
  })
}

data "aws_iam_policy_document" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  statement {
    actions = ["sqs:SendMessage"]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    resources = [aws_sqs_queue.import[0].arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [module.platform.ssm.eft.bfd-sns-topic-arn.value]
    }
  }
}

resource "aws_sqs_queue_policy" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  queue_url = aws_sqs_queue.import[0].id
  policy    = data.aws_iam_policy_document.import[0].json
}

resource "aws_sns_topic_subscription" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  endpoint  = aws_sqs_queue.import[0].arn
  protocol  = "sqs"
  topic_arn = module.platform.ssm.eft.bfd-sns-topic-arn.value
}

resource "aws_lambda_event_source_mapping" "import" {
  count = contains(["prod", "test"], local.env) ? 1 : 0

  event_source_arn = aws_sqs_queue.import[0].arn
  function_name    = aws_lambda_function.import[0].function_name
  batch_size       = 1
  enabled          = true
}
