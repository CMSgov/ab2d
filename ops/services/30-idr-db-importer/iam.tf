# Database IAM role
## enables RDS to pull data from S3 Bucket with IDR data
resource "aws_iam_role" "database_s3_import" {
  name = "${module.platform.app}-${module.platform.env}-database-s3-import"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = [
            "rds.amazonaws.com"
          ]
        }
        Condition = {
          "StringLike" = {
            "aws:SourceAccount" = "${local.aws_account_number}"
          }
        }
      },
    ]
  })
}

resource "aws_iam_policy" "database_s3_import" {
  name        = "${module.platform.app}-${module.platform.env}-database-s3-import"
  description = "${local.service_prefix} RDS access to handle S3 imports."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3Import"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          module.idr_db_importer_bucket.arn,
          "${module.idr_db_importer_bucket.arn}/*"
        ]
      },
      {
        Sid    = "SharedKeyAccess"
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey"
        ]
        Resource = module.platform.kms_alias_primary.target_key_arn
      }
    ]
  })
}

resource "aws_rds_cluster_role_association" "database_s3_import" {
  db_cluster_identifier = data.aws_rds_cluster.this.id
  feature_name          = "s3Import"
  role_arn              = aws_iam_role.database_s3_import.arn
}
