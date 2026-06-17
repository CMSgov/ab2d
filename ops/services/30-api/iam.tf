# -------------------------------------------------------
# Additional Task Policies —
# -------------------------------------------------------
data "aws_iam_role" "api" {
  name = "${local.service_prefix}-${local.service}"
}

resource "aws_iam_policy" "api" {
  name        = "ab2d-${module.platform.env}-api-additional-policy"
  description = "Additional IAM permissions for the AB2D API module beyond the base service and platform modules"
  policy      = data.aws_iam_policy_document.api.json
}

data "aws_iam_policy_document" "api" {
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
      values   = ["ab2d/${module.platform.env}/api"]
    }
  }
}
