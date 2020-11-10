resource "aws_kms_key" "test_terraform_state_kms_key" {
  description             = "${var.env}-test-terraform-state-kms"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}

resource "aws_kms_alias" "test_terraform_state_kms_key_alias" {
  name          = "alias/${var.env}-test-terraform-state-kms"
  target_key_id = aws_kms_key.test_terraform_state_kms_key.key_id
}

resource "aws_s3_bucket" "test_terraform_state_log_bucket" {
  bucket = "${var.env}-test-terraform-state-server-access-logs"
  acl    = "log-delivery-write"
}

data "aws_iam_policy_document" "test_terraform_state_bucket_policy" {  
  statement {
    sid    = "DenyUnencryptedObjectUploads"
    effect = "Deny"
    resources = ["arn:aws:s3:::${var.env}-test-terraform-state/*"]
    
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = [
        "firehose.amazonaws.com"
      ]
    }
  }
}

resource "aws_s3_bucket" "test_terraform_state_bucket" {
  bucket = "${var.env}-test-terraform-state"
  acl    = "private"

  logging {
    target_bucket = aws_s3_bucket.test_terraform_state_log_bucket.id
    target_prefix = "log/"
  }

  policy = "${data.aws_iam_policy_document.test_terraform_state_bucket_policy.json}"
  
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
#             "Resource": "arn:aws:s3:::${var.env}-test-terraform-state/*",
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
#             "Resource": "arn:aws:s3:::${var.env}-test-terraform-state/*",
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
#             "Resource": "arn:aws:s3:::${var.env}-test-terraform-state/*",
#             "Condition": {
#                 "ArnEquals": {
#                     "aws:userid": "arn:aws:iam::${var.aws_account_number}:role/ct-ado-ab2d-application-admin"
#                 }
#             }
#         }
#     ]
# }
# EOF
