# ab2d_mgmt_role_arn

# {
#   "Version": "2012-10-17",
#   "Statement": {
#     "Effect": "Allow",
#     "Action": "sts:AssumeRole",
#     "Resource": "arn:aws:iam::PRODUCTION-ACCOUNT-ID:role/UpdateApp"
#   }
# }

#
# Create automation role that will be used by management account for automation
#

data "aws_iam_policy_document" "allow_assume_role_in_target_account_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "AWS"
      identifiers = ["${var.ab2d_mgmt_role_arn}"]
    }
  }
}

resource "aws_iam_role" "ab2d_automation_role" {
  name               = "Ab2dAutomationRole"
  assume_role_policy = "${data.aws_iam_policy_document.allow_assume_role_in_target_account_policy.json}"
}

data "aws_iam_policy" "cms_approved_aws_services_in_mgmt" {
  arn = "arn:aws:iam::${var.mgmt_aws_account_number}:policy/CMSApprovedAWSServices"
}

resource "aws_iam_role_policy_attachment" "cms_approved_aws_services_attach" {
  role       = "${aws_iam_role.ab2d_automation_role.name}"
  policy_arn = "${data.aws_iam_policy.cms_approved_aws_services_in_mgmt.arn}"
}
