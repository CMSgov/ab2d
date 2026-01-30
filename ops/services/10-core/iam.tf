data "aws_iam_policy" "cms_cloud_ssm_iam" {
  arn = "arn:aws:iam::${local.aws_account_number}:policy/cms-cloud-ssm-iam-policy-v3"
}

data "aws_iam_policy" "developer_boundary_policy" {
  name = "developer-boundary-policy"
}

# WORKER
resource "aws_iam_role" "worker" {
  name                 = "${local.service_prefix}-worker"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "vpc-flow-logs.amazonaws.com",
            "s3.amazonaws.com",
            "ec2.amazonaws.com",
            "ecs-tasks.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "worker" {
  for_each = {
    a = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role",
    b = "arn:aws:iam::aws:policy/AmazonECS_FullAccess",
    c = "arn:aws:iam::aws:policy/AmazonAPIGatewayInvokeFullAccess",
    d = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    e = "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    f = "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    g = "arn:aws:iam::aws:policy/SecretsManagerReadWrite",
    h = "arn:aws:iam::aws:policy/EC2InstanceProfileForImageBuilderECRContainerBuilds",
    i = "arn:aws:iam::aws:policy/AmazonSQSFullAccess",
    j = "arn:aws:iam::aws:policy/AmazonSNSFullAccess",
    k = "arn:aws:iam::aws:policy/AmazonEC2FullAccess",
    l = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    m = data.aws_iam_policy.cms_cloud_ssm_iam.arn
    n = aws_iam_policy.microservices.arn
  }

  role       = aws_iam_role.worker.name
  policy_arn = each.value
}

# API
resource "aws_iam_role" "api" {
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn
  name                 = "${local.service_prefix}-api"
  path                 = "/delegatedadmin/developer/"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "vpc-flow-logs.amazonaws.com",
            "s3.amazonaws.com",
            "ec2.amazonaws.com",
            "ecs-tasks.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "api" {
  for_each = {
    a = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role",
    b = "arn:aws:iam::aws:policy/AmazonECS_FullAccess",
    c = "arn:aws:iam::aws:policy/AmazonAPIGatewayInvokeFullAccess",
    d = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    e = "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    f = "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    g = "arn:aws:iam::aws:policy/SecretsManagerReadWrite",
    h = "arn:aws:iam::aws:policy/EC2InstanceProfileForImageBuilderECRContainerBuilds",
    i = "arn:aws:iam::aws:policy/AmazonSQSFullAccess",
    j = "arn:aws:iam::aws:policy/AmazonEC2FullAccess",
    k = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    l = data.aws_iam_policy.cms_cloud_ssm_iam.arn,
    m = aws_iam_policy.microservices.arn
  }

  role       = aws_iam_role.api.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "microservices" {
  statement {
    actions = [
      "kms:*"
    ]
    resources = [
      local.env_key_alias.target_key_arn
    ]
  }
  statement {
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      "arn:aws:ssm:${local.region_name}:${local.aws_account_number}:parameter/ab2d/${local.env}/*"
    ]
  }
}

resource "aws_iam_policy" "microservices" {
  name = "${local.service_prefix}-microservices-kms"
  path = "/delegatedadmin/developer/"

  policy = data.aws_iam_policy_document.microservices.json
}

# MICROSERVICES
resource "aws_iam_role" "microservices" {
  #TODO consider simplifying the human readable name
  name                 = "${local.service_prefix}-microservices"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "ecs-tasks.amazonaws.com",
            "ec2.amazonaws.com",
            "s3.amazonaws.com",
            "chatbot.amazonaws.com",
            "lambda.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "microservices" {
  for_each = {
    a = "arn:aws:iam::aws:policy/AmazonSQSFullAccess"
    b = "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
    c = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
    d = "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser"
    e = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
    f = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    g = "arn:aws:iam::aws:policy/AmazonECS_FullAccess"
    h = "arn:aws:iam::aws:policy/EC2InstanceProfileForImageBuilderECRContainerBuilds"
    i = "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess"
    j = aws_iam_policy.microservices.arn
  }

  role       = aws_iam_role.microservices.name
  policy_arn = each.value
}

# IDR-DB-IMPORTER
resource "aws_iam_role" "idr-db-importer-execution" {
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn
  name                 = "${local.service_prefix}-idr-db-importer-execution"
  path                 = "/delegatedadmin/developer/"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "ecs-tasks.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_policy" "idr-db-importer-execution" {
  name = "${local.app}-${local.parent_env}-idr-db-importer-execution"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "ssm:GetParameters",
          "logs:PutLogEvents",
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "ecr:GetDownloadUrlForLayer",
          "ecr:GetAuthorizationToken",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability"
        ]
        Effect   = "Allow"
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "idr-db-importer-execution" {
  role       = aws_iam_role.idr-db-importer-execution.name
  policy_arn = aws_iam_policy.idr-db-importer-execution.arn
}

resource "aws_iam_role" "idr-db-importer" {
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn
  name                 = "${local.service_prefix}-idr-db-importer"
  path                 = "/delegatedadmin/developer/"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "ecs-tasks.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "idr-db-importer" {
  for_each = {
    a = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role",
    b = "arn:aws:iam::aws:policy/AmazonECS_FullAccess",
    c = "arn:aws:iam::aws:policy/AmazonAPIGatewayInvokeFullAccess",
    d = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    e = "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    f = "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    g = "arn:aws:iam::aws:policy/SecretsManagerReadWrite",
    h = "arn:aws:iam::aws:policy/EC2InstanceProfileForImageBuilderECRContainerBuilds",
    i = "arn:aws:iam::aws:policy/AmazonSQSFullAccess",
    j = "arn:aws:iam::aws:policy/AmazonEC2FullAccess",
    k = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    l = data.aws_iam_policy.cms_cloud_ssm_iam.arn
  }

  role       = aws_iam_role.idr-db-importer.name
  policy_arn = each.value
}

# Create KMS policy
resource "aws_iam_policy" "kms" {
  name   = "${local.service_prefix}-kms"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.kms.json
}

data "aws_iam_policy_document" "kms" {
  statement {
    sid = "AllowEnvCMKAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:ReEncryptFrom",
      "kms:ReEncryptTo",
      "kms:DescribeKey",
      "kms:CreateGrant",
      "kms:ListGrants",
      "kms:RevokeGrant"
    ]
    resources = [local.env_key_alias.target_key_arn]
  }
}

data "aws_iam_policy_document" "kms_key_access" {
  statement {
    sid = "AllowEnvCMKAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:ReEncrypt",
      "kms:DescribeKey",
      "kms:Encrypt"
    ]
    resources = [local.env_key_alias.target_key_arn]
  }
}

resource "aws_iam_policy" "kms_key_access" {
  name        = "${local.service_prefix}-${local.service}-kms-key-access"
  path        = "/delegatedadmin/developer/"
  description = "Permissions to access environment ${local.env} KMS CMK"
  policy      = data.aws_iam_policy_document.kms_key_access.json
}

data "aws_iam_policy" "rds_monitoring" {
  name = "AmazonRDSEnhancedMonitoringRole"
}

data "aws_iam_policy_document" "rds_monitoring_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["monitoring.rds.amazonaws.com"]
    }
  }
}

# Role allowing monitoring for RDS Clusters and instances
resource "aws_iam_role" "db_monitoring" {
  name                 = "${local.service_prefix}-rds-monitoring"
  assume_role_policy   = data.aws_iam_policy_document.rds_monitoring_assume.json
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn

}

resource "aws_iam_role_policy_attachment" "db_monitoring" {
  role       = aws_iam_role.db_monitoring.name
  policy_arn = data.aws_iam_policy.rds_monitoring.arn
}

resource "aws_iam_role_policy_attachment" "db_monitoring_kms" {
  role       = aws_iam_role.db_monitoring.name
  policy_arn = aws_iam_policy.kms_key_access.arn
}

resource "aws_iam_policy" "idr_db_importer" {
  name        = "${module.platform.app}-${module.platform.env}-idr_db_importer"
  description = "Aurora import S3 bucket access."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid = "s3import"
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Effect = "Allow"
        Resource = [
          "${module.idr_db_importer_bucket.arn}",
          "${module.idr_db_importer_bucket.arn}/*"
        ]
      },
      {
        Sid = "sharedKeyAccess"
        Action = [
          "kms:Decrypt",
          "kms:GenerateKeyData"
        ]
        Effect   = "Allow"
        Resource = "${module.platform.kms_alias_primary.target_key_arn}"
      }
    ]
  })
}

resource "aws_iam_role" "idr_db_importer" {
  name = "${module.platform.app}-${module.platform.env}-idr_db_importer"

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

resource "aws_iam_role_policy_attachment" "idr_db_importer" {
  role       = aws_iam_role.idr_db_importer.name
  policy_arn = aws_iam_policy.idr_db_importer.arn
}

resource "aws_rds_cluster_role_association" "idr_db_importer" {
  db_cluster_identifier = module.db.aurora_cluster.id
  feature_name          = "s3Import"
  role_arn              = aws_iam_role.idr_db_importer.arn
}
