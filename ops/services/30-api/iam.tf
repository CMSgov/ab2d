data "aws_iam_role" "api" {
  name = "${local.service_prefix}-${local.service}"
}
