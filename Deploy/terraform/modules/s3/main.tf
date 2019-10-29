resource "aws_s3_bucket" "fileservices" {
  bucket = var.bucket_name
  acl    = "private"
  versioning {enabled = true}
  logging {
    target_bucket = var.logging_bucket_name
    target_prefix = "cms-ab2d-${var.env}/server_access/"
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = var.encryption_key_arn
        sse_algorithm     = "aws:kms"
      }
    }
  }

  policy = <<POLICY
{
     "Version": "2012-10-17",
     "Statement": [
         {
             "Effect": "Deny",
             "Principal": "*",
             "Action": "s3:*",
             "Resource": [
                 "arn:aws:s3:::${var.bucket_name}"
             ],
             "Condition": {
                 "StringNotLike": {
                     "aws:username": ["${join("\",\"",var.username_list)}"]
                 }
             }
         },
         {
             "Effect": "Deny",
             "Principal": "*",
             "Action": "s3:*",
             "Resource": [
                 "arn:aws:s3:::${var.bucket_name}"
             ],
             "Condition": {
                 "StringNotLike": {
                     "aws:sourceVpce": "${var.vpc_id}",
                     "aws:username": ["${join("\",\"",var.username_list)}"]
                 }
             }
         }
     ]
  }
POLICY
}
