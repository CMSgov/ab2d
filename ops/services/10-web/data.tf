data "aws_wafv2_web_acl" "cms_waf_cdn" {
  scope = "CLOUDFRONT"
  name  = "SamQuickACLEnforcingV2"
}

#TODO consider defining this certificate as part of this module
# if SSM-stored keys/certificates is part of 00-bootstrap, this can remain as 10-web
# otherwise, 20-web is probably more appropriate if 10-config is home to the certificate materials
# data "aws_acm_certificate" "cdn_ssl" {
#   count    = local.env == "prod" ? 1 : 0
#   domain   = "ab2d.cms.gov"
#   statuses = ["ISSUED"]
# }


resource "aws_acm_certificate" "this" {
  count            = local.tls_private_key != null ? 1 : 0
  private_key      = local.tls_private_key
  certificate_body = local.tls_public_cert
}
