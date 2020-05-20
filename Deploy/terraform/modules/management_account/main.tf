# Create Ab2dAssumePolicy

data "aws_iam_policy_document" "allow_assume_role_in_target_account_policy" {
  statement {
    sid = "1"

    actions = [
      "sts:AssumeRole"
    ]

    resources = "${var.mgmt_target_aws_account_mgmt_roles}"
  }
}

resource "aws_iam_policy" "assume_policy" {
  name   = "Ab2dAssumePolicy"
  policy = "${data.aws_iam_policy_document.allow_assume_role_in_target_account_policy.json}"
}

# Attach Ab2dAssumePolicy to Ab2dInstanceRole

resource "aws_iam_role_policy_attachment" "instance_role_assume_policy_attach" {
  role       = "Ab2dInstanceRole"
  policy_arn = "${aws_iam_policy.assume_policy.arn}"
}

# Create "Ab2dMgmtRole" for the management account

data "aws_iam_policy_document" "allow_assume_role_in_mgmt_account_policy" {
  statement {
    sid = "1"

    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type        = "AWS"
      identifiers = [
        "arn:aws:iam::${var.mgmt_aws_account_number}:root"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_mgmt_role" {
  name               = "Ab2dMgmtRole"
  assume_role_policy = "${data.aws_iam_policy_document.allow_assume_role_in_mgmt_account_policy.json}"
}

resource "aws_iam_role_policy_attachment" "mgmt_role_assume_policy_attach" {
  for_each   = toset(var.ab2d_spe_developer_policies)
  role       = "${aws_iam_role.ab2d_mgmt_role.name}"
  policy_arn = "${each.value}"
}
