# Database IAM role
## enables RDS to pull data from S3 Bucket with IDR data
resource "aws_iam_role" "database_import_s3" {
  name                 = "${module.platform.app}-${module.platform.env}-database-import-s3"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn

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
            "aws:SourceAccount" = module.platform.aws_caller_identity.account_id
          }
        }
      },
    ]
  })
}

data "aws_iam_policy_document" "database_import_s3" {
  statement {
    sid = "S3Import"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]

    resources = [
      module.idr_db_importer_bucket.arn,
      "${module.idr_db_importer_bucket.arn}/*"
    ]
  }

  statement {
    sid = "SharedKeyAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey"
    ]

    resources = [
      module.platform.kms_alias_primary.target_key_arn
    ]
  }
}

resource "aws_iam_policy" "database_import_s3" {
  name        = "${module.platform.app}-${module.platform.env}-database-import-s3"
  description = "Aurora s3Import access to the IDR DB Importer bucket."
  policy      = data.aws_iam_policy_document.database_import_s3.json
}

resource "aws_iam_role_policy_attachment" "database_import_s3" {
  role       = aws_iam_role.database_import_s3.name
  policy_arn = aws_iam_policy.database_import_s3.arn
}

resource "aws_rds_cluster_role_association" "database_import_s3" {
  db_cluster_identifier = data.aws_rds_cluster.this.id
  feature_name          = "s3Import"
  role_arn              = aws_iam_role.database_import_s3.arn
}

# Task policies
## Manages resources to s3
data "aws_iam_policy_document" "idr_db_importer_bucket_write_and_read" {

  statement {
    sid = "S3Operations"
    actions = [
      "s3:AbortMultipartUpload",
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:ListBucket"
    ]

    resources = [
      module.idr_db_importer_bucket.arn,
      "${module.idr_db_importer_bucket.arn}/*"
    ]
  }

  statement {
    sid = "DecryptKMS"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey"
    ]

    resources = [
      module.platform.kms_alias_primary.target_key_arn
    ]
  }
}

resource "aws_iam_policy" "idr_db_importer_bucket_write_and_read" {
  name        = "${local.app}-${local.env}-idr-s3-bucket-write-and-read"
  description = "IDR DB Importer ECS task access to S3 bucket and encrypt resources."
  policy      = data.aws_iam_policy_document.idr_db_importer_bucket_write_and_read.json
}

data "aws_iam_policy_document" "idr_db_importer_bucket_extended_deny_policy" {
  statement {
    sid    = "DenyObjectAccessExceptIDRDBImporterRoles"
    effect = "Deny"

    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:AbortMultipartUpload"
    ]

    condition {
      test     = "StringNotEquals"
      variable = "aws:PrincipalArn"
      values = [
        module.service.task_role_arn,
        aws_iam_role.database_import_s3.arn
      ]
    }

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }

    resources = [
      module.idr_db_importer_bucket.arn,
      "${module.idr_db_importer_bucket.arn}/*",
    ]
  }
}

# Event scheduler
resource "aws_iam_role" "idr_db_importer_eventbridge_scheduler" {
  name = "${local.service_prefix}-idr-db-importer-cron-scheduler-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = ["scheduler.amazonaws.com"]
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_policy" "idr_db_importer_eventbridge_scheduler" {
  name = "${local.service_prefix}-idr-db-importer-cron-scheduler-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "ecs:RunTask"
        ],
        Resource = [
          trimsuffix(module.service.task_definition.arn, ":${module.service.task_definition.revision}"),
          "${trimsuffix(module.service.task_definition.arn, ":${module.service.task_definition.revision}")}:*"
        ],
        Condition = {
          "ArnLike" = {
            "ecs:cluster" = "${data.aws_ecs_cluster.shared.arn}"
          }
        }
      },
      {
        Effect = "Allow",
        Action = [
          "iam:PassRole"
        ]
        Resource = [
          module.service.task_role_arn,
          module.service.task_definition.execution_role_arn,
        ]
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "idr_db_importer_eventbridge_scheduler" {
  policy_arn = aws_iam_policy.idr_db_importer_eventbridge_scheduler.arn
  role       = aws_iam_role.idr_db_importer_eventbridge_scheduler.name
}
