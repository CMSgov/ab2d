# -------------------------------------------------------
# Additional Task Policies —
# -------------------------------------------------------
resource "aws_iam_policy" "events" {
  name        = "${module.platform.app}-${module.platform.env}-events"
  description = "Additional IAM permissions for the AB2D Events module beyond the base service and platform modules"
  policy      = data.aws_iam_policy_document.events.json
}

data "aws_iam_policy_document" "events" {
  statement {
    sid    = "SQSAccess"
    effect = "Allow"
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes"
    ]
    resources = [
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
      values   = ["ab2d/${module.platform.env}/events"]
    }
  }
}
