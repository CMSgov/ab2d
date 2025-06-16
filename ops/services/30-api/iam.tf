data "aws_iam_role" "api" {
  name = "${local.service_prefix}-${local.service}"
}

resource "aws_iam_instance_profile" "api_profile" { #FIXME: Delete this
  name = "${local.service_prefix}-api"
  path = "/delegatedadmin/developer/"
  role = data.aws_iam_role.api.name
}
