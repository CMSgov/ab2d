#
# BFD Insights
#

# Create Ab2dBfdInsightsV2Policy

data "aws_iam_policy_document" "bfd_insights_policy_document" {
  statement {
    actions = [
      "glue:GetTable",
      "glue:GetTableVersion",
      "glue:GetTableVersions"
    ]
    resources = [
      "*"
    ]
  }
  statement {
    actions = [
      "s3:AbortMultipartUpload",
      "s3:GetBucketLocation",
      "s3:GetObject",
      "s3:ListBucket",
      "s3:ListBucketMultipartUploads",
      "s3:PutObjectAcl",
      "s3:PutObject"
    ]
    resources = [
      "arn:aws:s3:::${var.ab2d_bfd_insights_s3_bucket}",
      "arn:aws:s3:::${var.ab2d_bfd_insights_s3_bucket}/*"
    ]
  }
  statement {
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey"
    ]
    resources = [
      var.ab2d_bfd_kms_arn
    ]
  }
  statement {
    actions = [
      "logs:PutLogEvents"
    ]
    resources = [
      "arn:aws:logs:us-east-1:${var.aws_account_number}:log-group:/aws/kinesisfirehose/bfd-insights-ab2d:log-stream:*"
    ]
  }
  statement {
    actions = [
      "kinesis:DescribeStream",
      "kinesis:GetShardIterator",
      "kinesis:GetRecords",
      "kinesis:ListShards"
    ]
    resources = [
      "*"
    ]
  }
}

resource "aws_iam_policy" "bfd_insights_policy" {
  name   = "Ab2dBfdInsightsV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.bfd_insights_policy_document.json
}

# Create Ab2dBfdInsightsV2Role

data "aws_iam_policy_document" "bfd_insights_role_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = [
        "firehose.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_bfd_insights_role" {
  name               = "Ab2dBfdInsightsV2Role"
  path               = "/delegatedadmin/developer/"
  assume_role_policy = data.aws_iam_policy_document.bfd_insights_role_assume_role_policy.json
  permissions_boundary = "arn:aws:iam::${var.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}

# Attach Ab2dBfdInsightsV2Policy to Ab2dInstanceV2Role

resource "aws_iam_role_policy_attachment" "instance_role_bfd_insights_policy_attach" {
  role       = aws_iam_role.ab2d_bfd_insights_role.name
  policy_arn = aws_iam_policy.bfd_insights_policy.arn
}

# Get instance role

data "aws_iam_role" "ab2d_instance_role" {
  name = "Ab2dInstanceV2Role"
}

# Attach Ab2dBfdInsightsV2Policy to Ab2dBfdInsightsV2Role

resource "aws_iam_role_policy_attachment" "bfd_insights_role_bfd_insights_policy_attach" {
  role       = data.aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.bfd_insights_policy.arn
}
