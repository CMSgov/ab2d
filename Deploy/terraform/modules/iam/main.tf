data "aws_iam_policy_document" "mgmt_role_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${var.mgmt_aws_account_number}:root"]
    }
  }
}

data "aws_iam_policy" "cms_approved_aws_services" {
  arn = "arn:aws:iam::${var.aws_account_number}:policy/CMSApprovedAWSServices"
}

resource "aws_iam_role" "ab2d_mgmt_role" {
  name               = "Ab2dMgmtRole"
  assume_role_policy = "${data.aws_iam_policy_document.mgmt_role_assume_role_policy.json}"
}

resource "aws_iam_role_policy_attachment" "cms_approved_aws_services_attach" {
  role       = "${aws_iam_role.ab2d_mgmt_role.name}"
  policy_arn = "${data.aws_iam_policy.cms_approved_aws_services.arn}"
}
