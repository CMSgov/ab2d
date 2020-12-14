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
        "arn:aws:iam::${var.mgmt_aws_account_number}:role/ct-ado-ab2d-application-admin",
        "arn:aws:iam::${var.mgmt_aws_account_number}:role/delegatedadmin/developer/Ab2dInstanceV2Role"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_mgmt_role" {
  name                 = "Ab2dMgmtV2Role"
  path                 = "/delegatedadmin/developer/"
  assume_role_policy   = data.aws_iam_policy_document.allow_assume_role_in_mgmt_account_policy.json
  permissions_boundary = "arn:aws:iam::${var.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}

resource "aws_iam_role_policy_attachment" "mgmt_role_administrator_access_policy_attach" {
  role       = aws_iam_role.ab2d_mgmt_role.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}
