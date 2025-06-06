terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

#FIXME Most everything in the 10-web module could be wrapped in a well-defined CDAP-managed module.
module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = local.app
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/10-web"
  service     = local.service
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "web"

  caching_policy = {
    CachingDisabled  = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    CachingOptimized = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }
}

resource "aws_cloudfront_origin_access_control" "oac" {
  name                              = "${local.service_prefix}-origin-access-control"
  description                       = "${local.service_prefix} static website"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_function" "redirects" {
  name    = "${local.service_prefix}-redesign-redirects"
  runtime = "cloudfront-js-2.0"
  comment = "Handle cool URIs and redirects for the redesign"
  code    = file("${path.module}/redirects-function.js")
}

resource "aws_cloudfront_distribution" "s3_distribution" {
  origin {
    domain_name              = aws_s3_bucket.this.bucket_regional_domain_name
    origin_id                = "${local.service_prefix}-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }
  enabled             = true
  comment             = "Distribution for ${local.service_prefix} website"
  default_root_object = "index.html"
  is_ipv6_enabled     = true
  price_class         = "PriceClass_100"
  http_version        = "http2and3"
  web_acl_id          = data.aws_wafv2_web_acl.cms_waf_cdn.arn
  restrictions {
    geo_restriction {
      restriction_type = "whitelist"
      locations        = ["US"]
    }
  }

  viewer_certificate {
    acm_certificate_arn            = local.env == "prod" ? data.aws_acm_certificate.cdn_ssl[0].arn : null #TODO need to determine where the certificate is actually created
    cloudfront_default_certificate = local.env == "prod" ? false : true
    minimum_protocol_version       = local.env == "prod" ? "TLSv1.2_2021" : null
    ssl_support_method             = local.env == "prod" ? "sni-only" : null
  }

  default_cache_behavior {
    cache_policy_id        = local.env == "prod" ? local.caching_policy["CachingOptimized"] : local.caching_policy["CachingDisabled"]
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "${local.service_prefix}-origin"
    compress               = true
    viewer_protocol_policy = "redirect-to-https"

    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.redirects.arn
    }
  }

  custom_error_response {
    error_caching_min_ttl = 10
    error_code            = 403
    response_code         = 404
    response_page_path    = "/404.html"
  }
}

