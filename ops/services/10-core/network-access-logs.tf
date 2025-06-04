data "aws_elb_service_account" "this" {}
data "aws_iam_policy_document" "network_access_logs" {
  statement {
    principals {
      type        = "AWS"
      identifiers = [data.aws_elb_service_account.this.arn]
    }
    actions   = ["s3:PutObject"]
    effect    = "Allow"
    sid       = "AllowElasticLoadBalancerToWriteAccessLogs"
    resources = ["${aws_s3_bucket.network_access_logs.arn}/*"]
  }

  statement {
    sid = "AWSCloudTrailAclCheck20150319"
    principals {
      type = "Service"
      identifiers = [
        "cloudtrail.amazonaws.com"
      ]
    }
    actions = [
      "s3:GetBucketAcl"
    ]
    resources = [aws_s3_bucket.network_access_logs.arn]
  }

  statement {
    sid = "AWSCloudTrailWrite20150319"
    principals {
      type        = "Service"
      identifiers = ["cloudtrail.amazonaws.com"]
    }
    actions   = ["s3:PutObject"]
    resources = ["${aws_s3_bucket.network_access_logs.arn}/AWSLogs/${local.aws_account_number}/*"]
    condition {
      test     = "StringEquals"
      variable = "s3:x-amz-acl"
      values   = ["bucket-owner-full-control"]
    }
  }

  statement {
    sid    = "AllowSSLRequestsOnly"
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.network_access_logs.arn,
      "${aws_s3_bucket.network_access_logs.arn}/*"
    ]
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket" "network_access_logs" {
  bucket_prefix = "${local.service_prefix}-network-access-logs"
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "network_access_logs" {
  bucket = aws_s3_bucket.network_access_logs.id

  rule {
    apply_server_side_encryption_by_default {
      # https://docs.aws.amazon.com/elasticloadbalancing/latest/application/enable-access-logging.html#access-log-create-bucket
      # > "The only server-side encryption option that's supported is Amazon S3-managed keys (SSE-S3)"
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_policy" "network_access_logs" {
  bucket = aws_s3_bucket.network_access_logs.id
  policy = data.aws_iam_policy_document.network_access_logs.json
}

resource "aws_ssm_parameter" "network_access_logs" {
  name  = "/ab2d/${local.env}/core/nonsensitive/network-access-logs-bucket-name"
  value = aws_s3_bucket.network_access_logs.id
  type  = "String"
}
