# Create "Ab2dInstanceV2Role" for the management account

data "aws_iam_policy_document" "instance_role_assume_role_policy" {
  statement {
    sid = "1"

    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type        = "Service"
      identifiers = [
        "ec2.amazonaws.com",
	"s3.amazonaws.com",
	"vpc-flow-logs.amazonaws.com",
	"ecs-tasks.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_instance_role" {
  name               = "Ab2dInstanceV2Role"
  path               = "/delegatedadmin/developer/"
  assume_role_policy = "${data.aws_iam_policy_document.instance_role_assume_role_policy.json}"
  permissions_boundary = "arn:aws:iam::${var.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}

# Create Ab2dAssumeV2Policy

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
  name   = "Ab2dAssumeV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = "${data.aws_iam_policy_document.allow_assume_role_in_target_account_policy.json}"
}

# Attach Ab2dAssumeV2Policy to Ab2dInstanceV2Role

resource "aws_iam_role_policy_attachment" "instance_role_assume_policy_attach" {
  role       = "Ab2dInstanceV2Role"
  policy_arn = "${aws_iam_policy.assume_policy.arn}"
}

# Create "Ab2dMgmtV2Role" for the management account

data "aws_iam_policy_document" "allow_assume_role_in_mgmt_account_policy" {
  statement {
    sid = "1"

    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type        = "AWS"
      identifiers = [
        "arn:aws:iam::${var.mgmt_aws_account_number}:role/ct-ado-ab2d-application-admin"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_mgmt_role" {
  name               = "Ab2dMgmtV2Role"
  path               = "/delegatedadmin/developer/"
  assume_role_policy = "${data.aws_iam_policy_document.allow_assume_role_in_mgmt_account_policy.json}"
  permissions_boundary = "arn:aws:iam::${var.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}
