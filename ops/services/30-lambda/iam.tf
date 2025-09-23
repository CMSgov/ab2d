data "aws_iam_policy_document" "assume_lambda" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "slack_lambda" {
  name                 = "${local.service_prefix}-slack-lambda"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
  assume_role_policy   = data.aws_iam_policy_document.assume_lambda.json
}

resource "aws_iam_policy" "slack_lambda" {
  path = "/delegatedadmin/developer/"
  name = "${local.service_prefix}-slack-lambda"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "ec2:DescribeNetworkInterfaces",
          "ec2:CreateNetworkInterface",
          "ec2:DeleteNetworkInterface",
          "ec2:DescribeInstances",
          "ec2:AttachNetworkInterface"
        ]
        Effect   = "Allow"
        Resource = "*"
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "slack_lambda_policy_attachment" {
  role       = aws_iam_role.slack_lambda.name
  policy_arn = aws_iam_policy.slack_lambda.arn
}

resource "aws_iam_role" "metrics_transform" {
  name                 = "${local.service_prefix}-metrics-transform"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
  assume_role_policy   = data.aws_iam_policy_document.assume_lambda.json
}

data "aws_iam_policy" "kms_key_access" {
  name = "${local.service_prefix}-core-kms-key-access"
}

resource "aws_iam_role_policy_attachment" "metrics_transform_attachment" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AWSLambda_FullAccess",
    "arn:aws:iam::aws:policy/AmazonSQSFullAccess",
    "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    "arn:aws:iam::aws:policy/AmazonElasticFileSystemFullAccess",
    data.aws_iam_policy.kms_key_access.arn
  ])

  role       = aws_iam_role.metrics_transform.id
  policy_arn = each.value
}

resource "aws_iam_role" "microservices_lambda" {
  name                 = "${local.service_prefix}-microservices-lambda"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
  assume_role_policy   = data.aws_iam_policy_document.assume_lambda.json
}

resource "aws_iam_role_policy_attachment" "microservices_lambda" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AWSLambda_FullAccess",
    "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess",
    "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    "arn:aws:iam::aws:policy/AmazonElasticFileSystemFullAccess",
    "arn:aws:iam::aws:policy/AmazonElasticFileSystemClientFullAccess",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole",
    "arn:aws:iam::aws:policy/AmazonElasticFileSystemClientReadWriteAccess"
  ])

  role       = aws_iam_role.microservices_lambda.id
  policy_arn = each.value
}

resource "aws_iam_role" "lambda_database_sns_role" {
  name                 = "${local.service_prefix}-database-sns-lambda"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
  assume_role_policy   = data.aws_iam_policy_document.assume_lambda.json
}

resource "aws_iam_role_policy_attachment" "lambda_db_sns_attachment" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AWSLambda_FullAccess",
    "arn:aws:iam::aws:policy/AmazonRDSDataFullAccess",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
  ])

  role       = aws_iam_role.lambda_database_sns_role.id
  policy_arn = each.value
}

resource "aws_iam_role" "lambda_sns_role" {
  name                 = "${local.service_prefix}-hpms-count-lambda"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
  assume_role_policy   = data.aws_iam_policy_document.assume_lambda.json
}

resource "aws_iam_role_policy_attachment" "lambda_sns_attachment" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AmazonSNSFullAccess",
    "arn:aws:iam::aws:policy/AWSLambda_FullAccess",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
  ])

  role       = aws_iam_role.lambda_sns_role.id
  policy_arn = each.value
}

resource "aws_iam_role" "opt_out" {
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
  name                  = "${local.service_prefix}-opt-out-function"
  path                  = "/delegatedadmin/developer/"
  permissions_boundary  = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
}

resource "aws_iam_role_policy" "opt_out_assume_bucket_role" {
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

resource "aws_iam_role_policy" "import_default_function" {
  count = contains(["prod", "test"], local.env) ? 1 : 0
  name  = "default-function"
  role  = aws_iam_role.opt_out[0].id
  policy = jsonencode({
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
  })
}
