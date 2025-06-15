data "aws_iam_policy_document" "this" {
  statement {
    sid       = "DenyInsecureTransportOperations"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = [aws_s3_bucket.this.arn, "${aws_s3_bucket.this.arn}/*"]

    principals {
      identifiers = ["*"]
      type        = "*"
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
  statement {
    sid       = "AllowCloudFrontDistribution"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.this.arn}/*"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.s3_distribution.arn]
    }
  }
}

resource "aws_s3_bucket" "this" {
  bucket_prefix = "${local.service_prefix}-website"
}

resource "aws_s3_bucket_logging" "this" {
  bucket = aws_s3_bucket.this.bucket

  target_bucket = module.platform.logging_bucket.id
  target_prefix = aws_s3_bucket.this.bucket_prefix
}

resource "aws_s3_bucket_policy" "this" {
  bucket = aws_s3_bucket.this.bucket
  policy = data.aws_iam_policy_document.this.json
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.bucket

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.bucket
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_ssm_parameter" "bucket" {
  name  = "/ab2d/${local.env}/web/nonsensitive/s3-bucket"
  value = aws_s3_bucket.this.id
  type  = "String"
}
