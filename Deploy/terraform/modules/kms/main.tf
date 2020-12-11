#
# Create KMS key
#

# resource "aws_kms_key" "a" {
#   description = "ab2d-kms"
#   tags = {
#     Name = "ab2d-kms"
#   }
#   policy =<<EOF
# {
#     "Version": "2012-10-17",
#     "Id": "key-default-1",
#     "Statement": [
#         {
#             "Sid": "Enable IAM Permissions",
#             "Effect": "Allow",
#             "Principal": {
#                 "AWS": [
# 		    "arn:aws:iam::${var.aws_account_number}:root",
# 		    "arn:aws:iam::${var.aws_account_number}:role/ct-ado-ab2d-application-admin",
#                     "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole"
#                 ]
#             },
#             "Action": "kms:*",
#             "Resource": "*"
#         }
#     ]
# }
# EOF
# }

resource "aws_kms_key" "a" {
  description = "ab2d-kms"
  tags = {
    Name = "ab2d-kms"
  }
  policy =<<EOF
{
    "Version": "2012-10-17",
    "Id": "key-default-1",
    "Statement": [
        {
            "Sid": "Enable IAM Permissions",
            "Effect": "Allow",
            "Principal": {
                "AWS": [
                    "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/Ab2dMgmtV2Role",
                    "arn:aws:iam::${var.aws_account_number}:role/ct-ado-ab2d-application-admin",
                    "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/Ab2dInstanceV2Role"
                ]
            },
            "Action": "kms:*",
            "Resource": "*"
        }
    ]
}
EOF
}

resource "aws_kms_alias" "a" {
  name = "alias/ab2d-kms"
  target_key_id = aws_kms_key.a.key_id
}

#
# Create Ab2dKmsPolicy and attach it to Ab2dInstanceRole
#

data "aws_iam_policy_document" "instance_role_kms_policy" {
  statement {
    actions = [
      "kms:Decrypt"
    ]

    resources = [
      "${aws_kms_key.a.arn}"
    ]
  }
}

resource "aws_iam_policy" "kms_policy" {
  name   = "Ab2dKmsV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = "${data.aws_iam_policy_document.instance_role_kms_policy.json}"
}

resource "aws_iam_role_policy_attachment" "instance_role_kms_policy_attach" {
  role       = "${var.ab2d_instance_role_name}"
  policy_arn = "${aws_iam_policy.kms_policy.arn}"
}
