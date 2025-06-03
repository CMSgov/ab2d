data "aws_iam_role" "worker" {
  name = "${local.service_prefix}-worker"
}

resource "aws_iam_instance_profile" "worker_profile" {
  name = "${local.service_prefix}-worker"
  path = "/delegatedadmin/developer/"
  role = data.aws_iam_role.worker.name
}
