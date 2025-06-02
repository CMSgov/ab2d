data "aws_wafv2_web_acl" "cms_waf" {
  scope = "REGIONAL"
  name   = "RegSamQuickACLEnforcingV2"
}

data "aws_wafv2_web_acl" "cms_waf_cdn" {
  scope = "CLOUDFRONT"
  name   = "SamQuickACLEnforcingV2"
}

data "aws_acm_certificate" "cdn_ssl" {
  count = var.env == "ab2d-east-prod" ? 1 : 0
  domain    = "ab2d.cms.gov"
  statuses = ["ISSUED"]
}
