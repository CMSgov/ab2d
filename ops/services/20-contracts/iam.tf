# -------------------------------------------------------
# Additional Task Policies —
# -------------------------------------------------------
resource "aws_iam_policy" "contracts" {
  name        = "${module.platform.app}-${module.platform.env}-contracts"
  description = "Additional IAM permissions for the AB2D Contracts module beyond the base service and platform modules"
  policy      = data.aws_iam_policy_document.contracts.json
}

data "aws_iam_policy_document" "contracts" {
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
      values   = ["ab2d/${module.platform.env}/contracts"]
    }
  }
}
