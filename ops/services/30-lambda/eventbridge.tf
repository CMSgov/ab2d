resource "aws_cloudwatch_event_rule" "securityhub" {
  name        = "${local.service_prefix}-securityhub"
  description = "To detect security hub findings with HIGH severity"

  #TODO make this like others using hcl dsl for policies in terraform
  event_pattern = <<EOF
    {
    "source": ["aws.securityhub"],
    "detail-type": ["Security Hub Findings - Imported"],
    "detail": {
      "findings": {
        "Compliance": {
          "Status": ["FAILED"]
        },
        "Severity": {
          "Normalized": [{
            "numeric": [">=", 70]
          }]
        }
      }
    }
  }
EOF
}

resource "aws_cloudwatch_event_target" "securityhub_lambda" {
  rule      = aws_cloudwatch_event_rule.securityhub.name
  target_id = "SendToLambda"
  arn       = aws_lambda_function.securityhub.arn
}

resource "aws_lambda_function" "securityhub" {
  filename      = "${path.module}/securityhub.zip"
  function_name = "${local.service_prefix}-securityhub-notifier"
  handler       = "securityhub.lambda_handler"
  role          = aws_iam_role.securityhub_lambda.arn
  runtime       = "python3.12"
  timeout       = 600
  description   = "Lambda function that sends security hub findings to slack"

  environment {
    variables = {
      SLACK_WEBHOOK_URL = local.slack_webhook_ab2d_security
    }
  }

  #FIXME use the archive_file.securityhub resource instead of a string in the filename
  depends_on = [
    data.archive_file.securityhub
  ]
}

resource "aws_lambda_permission" "eventbridge_rule" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.securityhub.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.securityhub.arn
}

data "archive_file" "securityhub" {
  type        = "zip"
  source_file = "${path.root}/code/securityhub.py"
  output_path = "${path.root}/securityhub.zip"
}

resource "aws_iam_role" "securityhub_lambda" {
  name                 = "${local.service_prefix}-securityhub-lambda"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = "arn:aws:iam::${module.platform.aws_caller_identity.account_id}:policy/cms-cloud-admin/developer-boundary-policy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "lambda.amazonaws.com"
          ]
        }
      },
    ]
  })
}

#FIXME overly permissive
resource "aws_iam_policy" "securityhub_lambda" {
  name = "${local.service_prefix}-securityhub-lambda"

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
        Effect = "Allow"
        Resource = [
          "*"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "securityhub_lambda" {
  #TODO
  for_each = {
    a = "arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess"
    b = "arn:aws:iam::aws:policy/CloudWatchFullAccess"
    c = "arn:aws:iam::aws:policy/AWSSecurityHubFullAccess"
    d = "arn:aws:iam::aws:policy/service-role/AmazonSNSRole"
    e = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
    f = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
    g = aws_iam_policy.securityhub_lambda.arn
  }

  role       = aws_iam_role.securityhub_lambda.id
  policy_arn = each.value
}
