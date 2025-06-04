data "aws_iam_role" "api" {
  name = "${local.service_prefix}-api"
}

resource "aws_iam_instance_profile" "api_profile" {
  name = "${local.service_prefix}-api"
  path = "/delegatedadmin/developer/"
  role = data.aws_iam_role.api.name
}
