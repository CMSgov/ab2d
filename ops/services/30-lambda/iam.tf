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
  name                 = "slack-lambda-${local.env}"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
  assume_role_policy   = data.aws_iam_policy_document.assume_lambda.json
}

resource "aws_iam_policy" "slack_lambda" {
  path = "/delegatedadmin/developer/"
  name = "slack_lambda-${local.env}"
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
  name                 = "metrics-transform-${local.env}"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"
  assume_role_policy   = data.aws_iam_policy_document.assume_lambda.json
}

resource "aws_iam_role_policy_attachment" "metrics_transform_attachment" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AWSLambda_FullAccess",
    "arn:aws:iam::aws:policy/AmazonSQSFullAccess",
    "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    "arn:aws:iam::aws:policy/AmazonElasticFileSystemFullAccess"
  ])

  role       = aws_iam_role.metrics_transform.id
  policy_arn = each.value
}

resource "aws_iam_role" "microservices_lambda" {
  name                 = "${local.env}-microservices-lambda"
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
  name                 = "${local.env}-database-sns-lambda-role"
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
  name                 = "hpms-count-lambda-${local.env}"
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
