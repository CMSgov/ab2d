data "aws_iam_role" "worker" {
  name = "${local.service_prefix}-${local.service}"
}
