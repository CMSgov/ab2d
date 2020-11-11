data "aws_iam_policy_document" "test_terraform_state_kms_key_policy" {
  statement {
    sid    = "Enable IAM User Permissions"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = [
        "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/Ab2dMgmtV2Role",
	"arn:aws:iam::${var.aws_account_number}:role/ct-ado-ab2d-application-admin"
      ]
    }

    actions = [
      "kms:*"
    ]

    resources = [
      "*"
    ]

  }
}

resource "aws_kms_key" "test_terraform_state_kms_key" {
  description             = "${var.env}-terraform-state-kms"
  policy                  = data.aws_iam_policy_document.test_terraform_state_kms_key_policy.json
  deletion_window_in_days = 10
  enable_key_rotation     = true
}

  # {
  #   "Version": "2012-10-17",
  #   "Id": "key-default-1",
  #   "Statement": [
  #       {
  #           "Sid": "Enable IAM User Permissions",
  #           "Effect": "Allow",
  #           "Principal": {
  #               "AWS": "arn:aws:iam::595094747606:root"
  #           },
  #           "Action": "kms:*",
  #           "Resource": "*"
  #       }
  #   ]
  # }

resource "aws_kms_alias" "test_terraform_state_kms_key_alias" {
  name          = "alias/${var.env}-terraform-state-kms"
  target_key_id = aws_kms_key.test_terraform_state_kms_key.key_id
}

resource "aws_s3_bucket" "test_terraform_state_log_bucket" {
  bucket = "${var.env}-terraform-state-server-access-logs"
  acl    = "log-delivery-write"
}

data "aws_iam_policy_document" "test_terraform_state_bucket_policy" {  
  statement {
    sid    = "DenyUnencryptedObjectUploads"
    effect = "Deny"

    resources = [
      "arn:aws:s3:::${var.env}-terraform-state/*"
    ]

    actions = [
      "s3:PutObject"
    ]

    principals {
      type        = "*"
      identifiers = [
        "*"
      ]
    }

   condition {
      test     = "StringNotEquals"
      variable = "s3:x-amz-server-side-encryption"

      values = [
        "aws:kms"
      ]
    }
  }

  statement {
    sid    = "RoleAccess"
    effect = "Allow"

    resources = [
      "arn:aws:s3:::${var.env}-terraform-state/*"
    ]

    actions = [
      "s3:GetObject"
    ]

    principals {
      type        = "AWS"
      identifiers = [
        "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/Ab2dMgmtV2Role",
	"arn:aws:iam::${var.aws_account_number}:role/ct-ado-ab2d-application-admin"
      ]
    }
  }
}

   #  principals {
   #    type        = "*"
   #    identifiers = [
   #      "*"
   #    ]
   #  }

   # condition {
   #    test     = "ArnEquals"
   #    variable = "aws:userid"

   #    values = [
   #      "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/Ab2dMgmtV2Role",
   #      "arn:aws:iam::${var.aws_account_number}:role/ct-ado-ab2d-application-admin"
   #    ]
   #  }

resource "aws_s3_bucket" "test_terraform_state_bucket" {
  bucket = "${var.env}-terraform-state"
  acl    = "private"

  logging {
    target_bucket = aws_s3_bucket.test_terraform_state_log_bucket.id
    target_prefix = "log/"
  }

  policy = data.aws_iam_policy_document.test_terraform_state_bucket_policy.json
  
  versioning {
    enabled = true
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = aws_kms_key.test_terraform_state_kms_key.arn
        sse_algorithm     = "aws:kms"
      }
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_dynamodb_table" "test_terraform_state_table" {
  name           = "${var.env}-terraform-table"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }
}

#   policy = <<EOF
# {
#     "Version": "2012-10-17",
#     "Id": "PutObjPolicy",
#     "Statement": [
#         {
#             "Sid": "DenyUnEncryptedObjectUploads",
#             "Effect": "Deny",
#             "Principal": "*",
#             "Action": "s3:PutObject",
#             "Resource": "arn:aws:s3:::${var.env}-terraform-state/*",
#             "Condition": {
#                 "StringNotEquals": {
#                     "s3:x-amz-server-side-encryption": "aws:kms"
#                 }
#             }
#         },
#         {
#             "Sid": "ManagementRole",
#             "Effect": "Allow",
#             "Principal": "*",
#             "Action": "s3:GetObject",
#             "Resource": "arn:aws:s3:::${var.env}-terraform-state/*",
#             "Condition": {
#                 "ArnEquals": {
#                     "aws:userid": "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/Ab2dMgmtV2Role"
#                 }
#             }
#         },
#         {
#             "Sid": "FederatedLoginRole",
#             "Effect": "Allow",
#             "Principal": "*",
#             "Action": "s3:GetObject",
#             "Resource": "arn:aws:s3:::${var.env}-terraform-state/*",
#             "Condition": {
#                 "ArnEquals": {
#                     "aws:userid": "arn:aws:iam::${var.aws_account_number}:role/ct-ado-ab2d-application-admin"
#                 }
#             }
#         }
#     ]
# }
# EOF
