# -------------------------------------------------------
# Additional Task Policies —
# -------------------------------------------------------
data "aws_iam_role" "worker" {
  name = "${local.service_prefix}-${local.service}"
}

resource "aws_iam_policy" "worker" {
  name        = "ab2d-${module.platform.env}-worker"
  description = "Additional IAM permissions for the AB2D Worker module beyond the base service and platform modules"
  policy      = data.aws_iam_policy_document.worker.json
}

data "aws_iam_policy_document" "worker" {
  statement {
    sid    = "S3BucketAccess"
    effect = "Allow"
    actions = [
      "s3:ListBucket"
    ]
    resources = [
      "arn:aws:s3:::ab2d-${module.platform.env}-data"
    ]
  }

  statement {
    sid    = "S3ObjectAccess"
    effect = "Allow"
    actions = [
      "s3:PutObject",
      "s3:GetObject",
      "s3:DeleteObject",
      "s3:HeadObject"
    ]
    resources = [
      "arn:aws:s3:::ab2d-${module.platform.env}-data/*"
    ]
  }

  statement {
    sid    = "SQSAccess"
    effect = "Allow"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes"
    ]
    resources = [
      "arn:aws:sqs:${module.platform.primary_region.name}:${module.platform.aws_caller_identity.account_id}:ab2d-${module.platform.env}-worker",
      "arn:aws:sqs:${module.platform.primary_region.name}:${module.platform.aws_caller_identity.account_id}:ab2d-${module.platform.env}-events"
    ]
  }

  statement {
    sid    = "CloudWatchMetricsAccess"
    effect = "Allow"
    actions = [
      "cloudwatch:PutMetricData"
    ]
    resources = ["*"]
    condition {
      test     = "StringEquals"
      variable = "cloudwatch:namespace"
      values   = ["ab2d/${module.platform.env}/worker"]
    }
  }
}
