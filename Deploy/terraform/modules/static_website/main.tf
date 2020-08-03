resource "aws_s3_bucket" "static_website" {
  bucket = "${var.env}-website"
  acl    = "private"

  tags = {
    Name = "${var.env}-website"
  }
}

resource "aws_s3_bucket_public_access_block" "example" {
  bucket = "${aws_s3_bucket.static_website.id}"

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

locals {
  s3_origin_id = "S3-${var.env}-website"
}

data "aws_acm_certificate" "ab2d_cms_gov" {
  domain   = "ab2d.cms.gov"
  statuses = ["ISSUED"]
}

resource "aws_cloudfront_origin_access_identity" "origin_access_identity" {
  comment = "Generate an origin access identity"
}

resource "aws_s3_bucket_policy" "static_website" {
  bucket = "${aws_s3_bucket.static_website.id}"

  policy = <<POLICY
{
    "Version": "2008-10-17",
    "Id": "PolicyForCloudFrontPrivateContent",
    "Statement": [
        {
            "Sid": "1",
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity ${aws_cloudfront_origin_access_identity.origin_access_identity.id}"
            },
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::${var.env}-website/*"
        }
    ]
}
POLICY
}

resource "aws_cloudfront_distribution" "s3_distribution" {
  origin {
    domain_name = "${aws_s3_bucket.static_website.bucket_regional_domain_name}"
    origin_id   = "${local.s3_origin_id}"

    s3_origin_config {
      origin_access_identity = "${aws_cloudfront_origin_access_identity.origin_access_identity.cloudfront_access_identity_path}"
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"

  #
  # *** TO DO ***
  #
  # logging_config {
  #   include_cookies = false
  #   bucket          = "mylogs.s3.amazonaws.com"
  #   prefix          = "myprefix"
  # }

  # *** TO DO ***: Uncomment after switching website to production
  # aliases = ["ab2d.cms.gov"]

  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "${local.s3_origin_id}"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "allow-all"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  # PriceClass_100 (only US, Canada, and Europe edge locations)
  # PriceClass_200 (only US, Canada, Europe, Asia, Middle East, and Africa edge locations)
  # PriceClass_All (all edge locations)
  price_class = "PriceClass_All"

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }    
  }

  tags = {
    Environment = "${var.env}"
  }

  viewer_certificate {

    # *** TO DO ***: Comment after switching website to production
    cloudfront_default_certificate = true
    
    # *** TO DO ***: Uncomment after switching website to production
    # acm_certificate_arn = "${data.aws_acm_certificate.ab2d_cms_gov.arn}"
    # ssl_support_method  = "sni-only"
    
  }
}