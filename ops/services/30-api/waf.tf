locals {
  add_waf = (module.platform.is_ephemeral_env && local.parent_env == "prod") ? true : false
}

data "aws_wafv2_ip_set" "external_services" {
  count = local.add_waf ? 1 : 0
  name  = "external-services"
  scope = "REGIONAL"
}

resource "aws_wafv2_ip_set" "api_customers" {
  count = local.add_waf ? 1 : 0
  name               = "${local.service_prefix}-api-customers"
  description        = "IP ranges for customers of this API"
  scope              = "REGIONAL"
  ip_address_version = "IPV4"

  # Addresses will be managed outside of terraform.
  addresses = []

  lifecycle {
    ignore_changes = [
      addresses,
    ]
  }
}

module "aws_waf" {
  count = local.add_waf ? 1 : 0

  source = "github.com/CMSgov/cdap//terraform/modules/firewall?ref=cbd07ee078ecd379a32125b8354bd1ecaf5c275d"

  app  = "ab2d"
  env  = local.env
  name = "${local.service_prefix}-api"

  scope        = "REGIONAL"
  content_type = "APPLICATION_JSON"

  associated_resource_arn = aws_lb.ab2d_api.arn
  rate_limit              = 3000
  ip_sets = [
      one(data.aws_wafv2_ip_set.external_services).arn,
      one(aws_wafv2_ip_set.api_customers).arn
      ]
}
