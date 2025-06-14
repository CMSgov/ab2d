data "aws_wafv2_web_acl" "cms_waf_cdn" {
  scope = "CLOUDFRONT"
  name  = "SamQuickACLEnforcingV2"
}
